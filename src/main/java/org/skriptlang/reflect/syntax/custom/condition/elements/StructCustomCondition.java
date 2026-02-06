package org.skriptlang.reflect.syntax.custom.condition.elements;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.conditions.base.PropertyCondition.PropertyType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.util.Utils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.reflect.syntax.custom.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.custom.condition.CustomCondition;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.custom.shared.entry.PatternsEntryData;
import org.skriptlang.reflect.syntax.custom.shared.entry.TriggerEntryData;
import org.skriptlang.reflect.syntax.custom.shared.entry.UsableInEntryData;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StructCustomCondition extends CustomSyntaxStructure<CustomCondition> {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(StructCustomCondition.class)
			.supplier(StructCustomCondition::new)
			.addPattern("[:local] condition <.+>")
			.addPattern("[:local] condition")
			.addPattern("[:local] %classinfos% property condition [is|are] <.+>")
			.addPattern("[:local] %classinfos% property condition can <.+>")
			.addPattern("[:local] %classinfos% property condition (has|have) <.+>")
			.addPattern("[:local] %classinfos% property condition will <.+>")
			.entryValidator(EntryValidator.builder()
				.addEntryData(new UsableInEntryData("usable in", null, true))
				.addEntryData(new PatternsEntryData("patterns", null, true))
				.addEntryData(new TriggerEntryData("parse", null, true))
				.addEntryData(new TriggerEntryData("check", null, false))
				.build())
			.build());
	}

	private Literal<ClassInfo<?>> types;
	private PropertyType propertyType;
	private boolean property;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {
		if (!super.init(args, matchedPattern, parseResult, entryContainer))
			return false;
		if (matchedPattern >= 2) {
			property = true;
			propertyType = PropertyType.values()[matchedPattern - 2];
			//noinspection unchecked
			this.types = (Literal<ClassInfo<?>>) args[0];
			String types = Arrays.stream(this.types.getArray())
				.map(ClassInfo::getCodeName)
				.map(Utils::toEnglishPlural)
				.collect(Collectors.joining("/"));
			patterns = PropertyCondition.getPatterns(propertyType, patterns[0], types);
		}
		return true;
	}

	@Override
	public boolean load() {
		super.load();

		ParserInstance parser = getParser();
		parser.setCurrentEvent("custom condition check trigger", ConditionCheckEvent.class);
		customSyntax.checkTrigger(entryContainer.get("check", Trigger.class, false));
		parser.deleteCurrentEvent();

		return true;
	}

	@Override
	protected CustomCondition createCustomSyntax() {
		return new CustomCondition(
			patterns,
			hasParseSection,
			local ? getParser().getCurrentScript() : null,
			entryContainer.getOptional("usable in", Predicate.class, false),
			property
		);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		if (local)
			builder.append("local");
		if (property) {
			builder.append(types);
			builder.append("property condition");
			builder.append(switch (propertyType) {
				case BE -> "is";
				case CAN -> "can";
				case HAVE -> "has";
				case WILL -> "will";
			});
		} else {
			builder.append("condition");
		}
		if (!hasPatternsSection)
			builder.append(patterns[0]);
		return builder.toString();
	}

}
