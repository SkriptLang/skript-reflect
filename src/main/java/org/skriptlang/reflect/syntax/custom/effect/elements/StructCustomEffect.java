package org.skriptlang.reflect.syntax.custom.effect.elements;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.reflect.syntax.custom.effect.CustomEffectInfo;
import org.skriptlang.reflect.syntax.custom.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.custom.shared.entry.PatternsEntryData;
import org.skriptlang.reflect.syntax.custom.shared.entry.TriggerEntryData;
import org.skriptlang.reflect.syntax.custom.shared.entry.UsableInEntryData;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.function.Predicate;

public class StructCustomEffect extends CustomSyntaxStructure<CustomEffectInfo> {

	public static boolean customEffectsUsed = false;

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(StructCustomEffect.class)
			.supplier(StructCustomEffect::new)
			.addPattern("[:local] effect <.+>")
			.addPattern("[:local] effect")
			.entryValidator(EntryValidator.builder()
				.addEntryData(new UsableInEntryData("usable in", null, true))
				.addEntryData(new PatternsEntryData("patterns", null, true))
				.addEntryData(new TriggerEntryData("parse", null, true))
				.addEntryData(new TriggerEntryData("trigger", null, false))
				.build())
			.build());
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {
		if (super.init(args, matchedPattern, parseResult, entryContainer)) {
			customEffectsUsed = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean load() {
		super.load();

		ParserInstance parser = getParser();
		parser.setCurrentEvent("custom effect trigger", EffectTriggerEvent.class);
		customSyntaxInfo.executeTrigger(entryContainer.get("trigger", Trigger.class, false));
		parser.deleteCurrentEvent();

		return true;
	}

	@Override
	protected CustomEffectInfo createCustomSyntaxInfo() {
		return new CustomEffectInfo(
			patterns,
			hasParseSection,
			local ? getParser().getCurrentScript() : null,
			entryContainer.getOptional("usable in", Predicate.class, false)
		);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		if (local)
			builder.append("local");
		builder.append("effect");
		if (!hasPatternsSection)
			builder.append(patterns[0]);
		return builder.toString();
	}

}
