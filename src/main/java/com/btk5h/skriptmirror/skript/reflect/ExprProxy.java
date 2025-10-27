package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.FunctionWrapper;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.skript.reflect.sections.Section;
import com.btk5h.skriptmirror.skript.reflect.sections.SectionEvent;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExprProxy extends SimpleExpression<Object> {

	public static boolean proxiesUsed = false;

	static {
		Skript.registerExpression(ExprProxy.class, Object.class, ExpressionType.COMBINED,
				"[a] [new] proxy [instance] of %javatypes% (using|from) %objects%");
	}

	private Expression<JavaType> interfaces;
	private Variable<?> handler;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		proxiesUsed = true;

		interfaces = SkriptUtil.defendExpression(exprs[0]);
		if (interfaces instanceof Literal) {
			JavaType[] javaTypes = ((Literal<JavaType>) interfaces).getArray();
			for (JavaType javaType : javaTypes) {
				if (!javaType.getJavaClass().isInterface()) {
					Skript.warning(javaType + " is not an interface");
				}
			}
		}

		Expression<?> var = SkriptUtil.defendExpression(exprs[1]);
		if (var instanceof Variable && ((Variable<?>) var).isList()) {
			handler = (Variable<?>) var;
			return true;
		}

		Skript.error(var + " is not a list variable.");
		return false;
	}

	@Override
	protected Object[] get(Event e) {
		Map<String, FunctionWrapper> handlers = new HashMap<>();
		Map<String, Section> sectionHandlers = new HashMap<>();
		handler.variablesIterator(e)
			.forEachRemaining(pair -> {
				Object value = pair.getValue();
				if (value instanceof FunctionWrapper) {
					handlers.put(pair.getKey(), (FunctionWrapper) value);
				} else if (value instanceof Section) {
					sectionHandlers.put(pair.getKey(), (Section) value);
				}
			});

		return new Object[]{
			Proxy.newProxyInstance(
				LibraryLoader.getClassLoader(),
				Arrays.stream(interfaces.getArray(e))
					.map(JavaType::getJavaClass)
					.filter(Class::isInterface)
					.toArray(Class[]::new),
				new VariableInvocationHandler(handlers, sectionHandlers)
			)
		};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<?> getReturnType() {
		return Object.class;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return String.format("proxy of %s from %s",
			interfaces.toString(e, debug),
			handler.toString(e, debug));
	}

	private static class VariableInvocationHandler implements InvocationHandler {
		@Nullable
		private static final Method INVOKE_DEFAULT;
		static {
			Method method;
			try {
				//noinspection JavaReflectionMemberAccess
				method = InvocationHandler.class.getDeclaredMethod("invokeDefault", Object.class, Method.class, Object[].class);
			} catch (NoSuchMethodException e) {
				method = null;
			}
			INVOKE_DEFAULT = method;
		}

		private final Map<String, FunctionWrapper> handlers;
		private final Map<String, Section> sectionHandlers;

		public VariableInvocationHandler(Map<String, FunctionWrapper> handlers, Map<String, Section> sectionHandlers) {
			this.handlers = handlers;
			this.sectionHandlers = sectionHandlers;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] methodArgs) {
			FunctionWrapper functionWrapper = handlers.get(method.getName().toLowerCase());
			Section section = sectionHandlers.get(method.getName().toLowerCase());

			if (functionWrapper == null && section == null) {
				if (INVOKE_DEFAULT != null) {
					if (method.isDefault()) {
						try {
							return INVOKE_DEFAULT.invoke(this, proxy, method, methodArgs);
						} catch (IllegalAccessException | InvocationTargetException e) {
							throw new RuntimeException(e);
						}
					} else if (method.getName().equals("toString") && method.getParameterCount() == 0) {
						// Default impl of toString
						return proxy.getClass().getName() + "@" + Integer.toHexString(proxy.hashCode());
					} else if (method.getName().equals("hashCode") && method.getParameterCount() == 0) {
						// Default impl of hashCode
						return System.identityHashCode(proxy);
					} else if (method.getName().equals("equals")
							&& method.getParameterCount() == 1
							&& method.getParameterTypes()[0] == Object.class) {
						// Default impl of equals
						return proxy == methodArgs[0];
					}
				}

				return null;
			}

			Function<?> function = functionWrapper == null ? null : functionWrapper.getFunction();
			Object[] functionArgs = functionWrapper == null ? new Object[0] : functionWrapper.getArguments();

			if (functionWrapper != null && function == null) {
				return null;
			}

			if (methodArgs == null) {
				methodArgs = new Object[0];
			}

			List<Object[]> params = new ArrayList<>(functionArgs.length + methodArgs.length + 1);
			Arrays.stream(functionArgs)
				.map(arg -> new Object[]{arg})
				.forEach(params::add);
			params.add(new Object[]{proxy});
			Arrays.stream(methodArgs)
				.map(arg -> new Object[]{arg})
				.forEach(params::add);

			Object[] returnValueArray;
			if (function != null) {
				FunctionEvent<?> functionEvent = new FunctionEvent<>(function);

				Object[][] args = params.stream()
					.limit(function.getParameters().length)
					.toArray(Object[][]::new);

				returnValueArray = function.execute(functionEvent, args);
			} else {
				SectionEvent sectionEvent = section.run(params.toArray(new Object[0][]));
				returnValueArray = sectionEvent.getOutput();
			}

			Object returnValue = (returnValueArray == null || returnValueArray.length == 0) ? null :
				ObjectWrapper.unwrapIfNecessary(returnValueArray[0]);

			Class<?> returnType = method.getReturnType();
			if (returnType == void.class) {
				return null;
			} else {
				return JavaUtil.convert(returnValue, returnType);
			}
		}
	}

}
