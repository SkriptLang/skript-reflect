package org.skriptlang.reflect.syntax.custom.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.reflect.syntax.custom.event.CustomEventInfo;
import org.skriptlang.reflect.syntax.custom.event.CustomEventManager;
import org.skriptlang.reflect.syntax.custom.event.CustomEventManager.RegisteredEvent;
import org.skriptlang.reflect.syntax.custom.event.EventCheckEvent;
import org.skriptlang.reflect.syntax.custom.event.EventValuesEntryData;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.custom.shared.entry.PatternsEntryData;
import org.skriptlang.reflect.syntax.custom.shared.entry.TriggerEntryData;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class StructCustomEvent extends CustomSyntaxStructure<CustomEventInfo> {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(StructCustomEvent.class)
			.supplier(StructCustomEvent::new)
			.addPattern("[:local] [custom] event %string%")
			.entryValidator(EntryValidator.builder()
				.addEntry("pattern", null, true)
				.addEntryData(new PatternsEntryData("patterns", null, true))
				.addEntryData(new EventValuesEntryData("event values", Collections.emptyList(), true) {
					@Override
					public boolean canCreateWith(String node) {
						return super.canCreateWith(node)
							|| node.startsWith("event-values" + getSeparator());
					}
				})
				.addEntryData(new TriggerEntryData("parse", null, true))
				.addEntryData(new TriggerEntryData("check", null, true))
				.build())
			.build());
	}

	private Literal<String> identifier;
	private List<Class<?>> eventValueTypes;
	private WeakReference<RegisteredEvent> registeredEventRef;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {
		this.entryContainer = entryContainer;
		this.local = parseResult.hasTag("local");
		this.hasParseSection = entryContainer.hasEntry("parse");

		this.hasPatternsSection = entryContainer.hasEntry("patterns");
		if (hasPatternsSection && entryContainer.hasEntry("pattern")) {
			Skript.error("You cannot use both 'pattern' and 'patterns' entries in a custom event.");
			return false;
		}

		if (!hasPatternsSection && !entryContainer.hasEntry("pattern")) {
			Skript.error("You must define at least one pattern for a custom event.");
			return false;
		}

		patterns = hasPatternsSection
			? entryContainer.get("patterns", String[].class, false)
			: new String[] {entryContainer.get("pattern", String.class, false)};

		//noinspection unchecked
		identifier = (Literal<String>) args[0];
		eventValueTypes = entryContainer.getOptional("event values", List.class, true);

		String identifier = this.identifier.getSingle();
		if (CustomEventManager.isEventDefined(identifier)) {
			Skript.error("Custom event '" + identifier + "' is already registered.");
			return false;
		}

		registeredEventRef = new WeakReference<>(CustomEventManager.defineCustomEvent(identifier));

		return super.preLoad() && super.load();
	}

	@Override
	public boolean preLoad() {
		return true;
	}

	@Override
	public boolean load() {
		if (entryContainer.hasEntry("check")) {
			ParserInstance parser = getParser();
			parser.setCurrentEvent("custom event check trigger", EventCheckEvent.class);
			customSyntaxInfo.checkTrigger(entryContainer.get("check", Trigger.class, false));
			parser.deleteCurrentEvent();
		}

		return true;
	}

	@Override
	protected CustomEventInfo createCustomSyntaxInfo() {
		return new CustomEventInfo(
			patterns,
			hasParseSection,
			local ? getParser().getCurrentScript() : null,
			entryContainer.getOptional("usable in", Predicate.class, false),
			identifier.getSingle(),
			eventValueTypes,
			registeredEventRef.get()
		);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		if (local)
			builder.append("local");
		return builder.append("custom event", identifier).toString();
	}

}
