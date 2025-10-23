package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.Option;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.structures.StructOptions;
import ch.njol.skript.variables.Variables;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.event.elements.ExprReplacedEventValue;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class SkriptReflection {

	private static Field LOCAL_VARIABLES;
	private static Field NODES;
	private static Method VARIABLES_MAP_COPY;
	private static Field DEFAULT_EXPRESSION;
	private static Field PARSED_VALUE;
	private static Field OPTIONS;

	static {
		Field _FIELD;
		Method _METHOD;

		try {
			_FIELD = Variables.class.getDeclaredField("localVariables");
			_FIELD.setAccessible(true);
			LOCAL_VARIABLES = _FIELD;
		} catch (NoSuchFieldException e) {
			warning("Skript's local variables field could not be resolved.");
		}

		try {
			_FIELD = SectionNode.class.getDeclaredField("nodes");
			_FIELD.setAccessible(true);
			NODES = _FIELD;
		} catch (NoSuchFieldException e) {
			warning("Skript's nodes field could not be resolved, therefore custom syntax and sections won't work.");
		}

		try {
			Class<?> variablesMap = Class.forName("ch.njol.skript.variables.VariablesMap");

			try {
				_METHOD = variablesMap.getDeclaredMethod("copy");
				_METHOD.setAccessible(true);
				VARIABLES_MAP_COPY = _METHOD;
			} catch (NoSuchMethodException e) {
				warning("Skript's variables map 'copy' method could not be resolved.");
			}
		} catch (ClassNotFoundException e) {
			warning("Skript's variables map class could not be resolved.");
		}

		try {
			_FIELD = ClassInfo.class.getDeclaredField("defaultExpression");
			_FIELD.setAccessible(true);
			DEFAULT_EXPRESSION = _FIELD;
		} catch (NoSuchFieldException e) {
			warning("Skript's default expression field could not be resolved, " +
				"therefore event-values won't work in custom events");
		}

		try {
			_FIELD = Option.class.getDeclaredField("parsedValue");
			_FIELD.setAccessible(true);
			PARSED_VALUE = _FIELD;
		} catch (NoSuchFieldException e) {
			warning("Skript's parsed value field could not be resolved, " +
				"therefore and/or warnings won't be suppressed");
		}

		try {
			_FIELD = StructOptions.OptionsData.class.getDeclaredField("options");
			_FIELD.setAccessible(true);
			OPTIONS = _FIELD;
		} catch (NoSuchFieldException e) {
			warning("Skript's options field could not be resolved, computed options won't work");
		}
	}

	private static void warning(String message) {
		SkriptMirror.getInstance().getLogger().warning(message);
	}

	/**
	 * Sets the local variables of an {@link Event} to the given local variables.
	 */
	@SuppressWarnings("unchecked")
	public static void putLocals(Object originalVariablesMap, Event to) {
		if (originalVariablesMap == null) {
			removeLocals(to);
			return;
		}

		try {
			Map<Event, Object> localVariables = (Map<Event, Object>) LOCAL_VARIABLES.get(null);

			localVariables.put(to, originalVariablesMap);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Removes and returns the local variables from the given {@link Event}.
	 */
	@SuppressWarnings("unchecked")
	public static Object removeLocals(Event event) {
		try {
			Map<Event, Object> localVariables = (Map<Event, Object>) LOCAL_VARIABLES.get(null);
			return localVariables.remove(event);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves the local variables from an {@link Event}.
	 * @param event The {@link Event} to get the local variables from.
	 * @return The local variables of the given {@link Event}.
	 */
	@SuppressWarnings("unchecked")
	public static Object getLocals(Event event) {
		try {
			Map<Event, Object> localVariables = (Map<Event, Object>) LOCAL_VARIABLES.get(null);
			return localVariables.get(event);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Copies the VariablesMap contained in the given {@link Object}.
	 * @param locals The local variables to copy.
	 * @return The copied local variables.
	 */
	public static Object copyLocals(Object locals) {
		if (locals == null)
			return null;

		try {
			return VARIABLES_MAP_COPY.invoke(locals);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e); // setAccessible called
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves the {@link Node}s of a {@link SectionNode}.
	 * @param sectionNode The {@link SectionNode} to get the nodes from.
	 * @return The {@link Node}s of the given {@link SectionNode}
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<Node> getNodes(SectionNode sectionNode) {
		if (NODES == null)
			return new ArrayList<>();

		try {
			return (ArrayList<Node>) NODES.get(sectionNode);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Replaces the event-values of a list of {@link ClassInfo}s with
	 * {@link ExprReplacedEventValue}'s to make them work in custom events.
	 *
	 * @param classInfoList A list of {@link ClassInfo}s to replace
	 */
	public static void replaceEventValues(List<ClassInfo<?>> classInfoList) {
		if (DEFAULT_EXPRESSION == null)
			return;

		try {
			List<ClassInfo<?>> replaceExtraList = new ArrayList<>();
			for (ClassInfo<?> classInfo : classInfoList) {
				DefaultExpression<?> defaultExpression = classInfo.getDefaultExpression();
				if (defaultExpression instanceof EventValueExpression && !(defaultExpression instanceof ExprReplacedEventValue)) {
					DEFAULT_EXPRESSION.set(classInfo,
						new ExprReplacedEventValue<>((EventValueExpression<?>) defaultExpression));

					replaceExtraList.add(classInfo);
				}
			}

			replaceExtraList.forEach(SkriptReflection::replaceExtra);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Replaces {@link ClassInfo}s related to the given {@link ClassInfo}.
	 */
	public static void replaceExtra(ClassInfo<?> classInfo) {
		List<ClassInfo<?>> classInfoList = Classes.getClassInfos().stream()
			.filter(loopedClassInfo -> !(loopedClassInfo.getDefaultExpression() instanceof ExprReplacedEventValue))
			.filter(loopedClassInfo -> classInfo.getC().isAssignableFrom(loopedClassInfo.getC())
				|| loopedClassInfo.getC().isAssignableFrom(classInfo.getC()))
			.collect(Collectors.toList());
		replaceEventValues(classInfoList);
	}

	/**
	 * Disable Skript's missing and / or warnings.
	 */
	public static void disableAndOrWarnings() {
		if (PARSED_VALUE == null)
			return;

		Option<Boolean> option = SkriptConfig.disableMissingAndOrWarnings;
		if (!option.value()) {
			try {
				PARSED_VALUE.set(option, true);
			} catch (IllegalAccessException e) {
				throw new RuntimeException();
			}
		}
	}

	/**
	 * Gets the modifiable options map from an options data object.
	 *
	 * @param script the script to get the options from.
	 * @return the modifiable options map.
	 *
	 * @throws NullPointerException if the given options data object is null.
	 * @throws IllegalStateException if skript-reflect could not find the modifiable options map.
	 */
	public static Map<String, String> getOptions(Script script) {
		if (script == null)
			throw new NullPointerException();

		if (OPTIONS == null)
			throw new IllegalStateException("OPTIONS field not initialized, computed options cannot be used");

		StructOptions.OptionsData optionsData = script.getData(StructOptions.OptionsData.class,
			StructOptions.OptionsData::new);

		try {
			return (Map<String, String>) OPTIONS.get(optionsData);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e); // setAccessible called
		}
	}

}
