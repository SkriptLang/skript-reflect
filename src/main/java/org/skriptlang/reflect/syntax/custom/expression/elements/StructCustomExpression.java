package org.skriptlang.reflect.syntax.custom.expression.elements;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.DefaultClasses;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.reflect.syntax.custom.expression.*;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.custom.shared.entry.PatternsEntryData;
import org.skriptlang.reflect.syntax.custom.shared.entry.TriggerEntryData;
import org.skriptlang.reflect.syntax.custom.shared.entry.UsableInEntryData;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.EntryValidator.EntryValidatorBuilder;
import org.skriptlang.skript.lang.entry.SectionEntryData;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StructCustomExpression extends CustomSyntaxStructure<CustomExpression<?>> {

	private ChangerData changerData;

	public static void register(SyntaxRegistry registry) {
		EntryValidatorBuilder builder = EntryValidator.builder()
			.addEntryData(new ExpressionEntryData<>(
				"return type",
				new SimpleLiteral<>(DefaultClasses.OBJECT, true),
				true,
				SkriptParser.PARSE_LITERALS,
				ClassInfo.class
			))
			.addEntry("loop of", null, true)
			.addEntryData(new UsableInEntryData("usable in", null, true))
			.addEntryData(new PatternsEntryData("patterns", null, true))
			.addEntryData(new TriggerEntryData("parse", null, true))
			.addEntryData(new SectionEntryData("get", null, true));

		Arrays.stream(ChangeMode.values())
			.map(ChangeMode::name)
			.map(name -> name.toLowerCase(Locale.ENGLISH).replace('_', ' '))
			.sorted((a, b) -> {
				long wordsA = a.chars().filter(c -> c == ' ').count();
				long wordsB = b.chars().filter(c -> c == ' ').count();
				return Long.compare(wordsA, wordsB) * -1;
			})
			.forEach(mode -> builder.addEntryData(new ChangerEntryData(mode, true)));

		registry.register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(StructCustomExpression.class)
			.supplier(StructCustomExpression::new)
			.addPattern("[:local] [:plural|plural:non[-| ]single] expression <.+>")
			.addPattern("[:local] [:plural|plural:non[-| ]single] expression")
			.addPattern("[:local] [:plural|plural:non[-| ]single] %classinfos% [:default] property <.+>")
			.entryValidator(builder.build())
			.build());
	}

	private final Map<ChangeMode, ChangerNode> changeModes = new EnumMap<>(ChangeMode.class);
	private Class<?> returnType;
	private boolean single, property;
	private Literal<ClassInfo<?>> types;
	private boolean defaultPatterns;
	private String loopOf;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {
		if (!super.init(args, matchedPattern, parseResult, entryContainer))
			return false;
		this.returnType = ((ClassInfo<?>) entryContainer.get("return type", Literal.class, true).getSingle()).getC();
		this.single = !parseResult.hasTag("plural");

		if (matchedPattern != 2)
			return true;

		property = true;
		//noinspection unchecked
		this.types = (Literal<ClassInfo<?>>) args[0];
		String types = Arrays.stream(this.types.getArray())
			.map(ClassInfo::getCodeName)
			.map(Utils::toEnglishPlural)
			.collect(Collectors.joining("/"));
		this.defaultPatterns = parseResult.hasTag("default");
		this.patterns = defaultPatterns
			? PropertyExpression.getPatterns(patterns[0], types)
			: PropertyExpression.getDefaultPatterns(patterns[0], types);
		this.loopOf = entryContainer.getOptional("loop of", String.class, false);
		return true;
	}

	@Override
	public boolean preLoad() {
		for (ChangeMode mode : ChangeMode.values()) {
			String key = mode.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
			ChangerNode node = entryContainer.getOptional(key, ChangerNode.class, false);
			if (node == null)
				continue;
			changeModes.put(mode, node);
		}
		return super.preLoad();
	}

	@Override
	public boolean load() {
		super.load();

		ParserInstance parser = getParser();
		SectionNode node = entryContainer.getOptional("get", SectionNode.class, false);
		if (node != null) {
			parser.setCurrentEvent("custom expression get trigger", ExpressionGetEvent.class);

			Trigger getterTrigger = customSyntax.loadReturnableTrigger(
				node,
				"custom expression get trigger",
				new SimpleEvent()
			);
			customSyntax.getterTrigger(getterTrigger);
			parser.deleteCurrentEvent();
		}

		for (Map.Entry<ChangeMode, ChangerNode> entry : changeModes.entrySet()) {
			ChangeMode mode = entry.getKey();
			ChangerNode changerNode = entry.getValue();
			parser.setCurrentEvent("custom expression " + changerNode.name() + " trigger", ExpressionChangeEvent.class);
			changerData = new ChangerData(changerNode.acceptedClasses());
			Trigger trigger = new Trigger(
				parser.getCurrentScript(),
				"entry with key: " + changerNode.name(),
				new SimpleEvent(),
				ScriptLoader.loadItems(changerNode.node())
			);
			customSyntax.changerTrigger(mode, trigger, changerNode.acceptedClasses());
			parser.deleteCurrentEvent();
		}

		return true;
	}

	@Override
	protected CustomExpression<?> createCustomSyntax() {
		Map<ChangeMode, ChangerTrigger> changerTriggers = new EnumMap<>(ChangeMode.class);
		for (ChangeMode mode : changeModes.keySet())
			changerTriggers.put(mode, null);

		return new CustomExpression<>(
			patterns,
			hasParseSection,
			local ? getParser().getCurrentScript() : null,
			entryContainer.getOptional("usable in", Predicate.class, false),
			returnType,
			single,
			property,
			loopOf,
			changerTriggers
		);
	}

	public ChangerData changerData() {
		return changerData;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		if (local)
			builder.append("local");
		if (!single)
			builder.append("plural");
		if (types != null) {
			builder.append(types);
			if (defaultPatterns)
				builder.append("default");
			builder.append("property");
		} else {
			builder.append("expression");
		}
		if (!hasPatternsSection)
			builder.append(patterns[0]);
		return builder.toString();
	}

	public static class ChangerData {

		private final Class<?>[] acceptedClasses;
		private final Kleenean plural;

		public ChangerData(Class<?>[] acceptedClasses) {
			this.acceptedClasses = acceptedClasses;
			Kleenean plural = null;
			for (int i = 0; i < acceptedClasses.length; i++) {
				Class<?> type = acceptedClasses[i];
				boolean isArray = type.isArray();
				if (isArray)
					acceptedClasses[i] = type.componentType();
				plural = CustomSyntaxStructure.ExpressionsData.updatePlurality(plural, isArray);
			}

			this.plural = plural != null ? plural : Kleenean.UNKNOWN;
		}

		public Class<?>[] acceptedClasses() {
			return acceptedClasses;
		}

		public boolean testPlurality(boolean plural) {
			return switch (this.plural) {
				case UNKNOWN -> true;
				case TRUE -> plural;
				case  FALSE -> !plural;
			};
		}

	}

}
