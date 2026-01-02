package org.skriptlang.reflect.syntax.custom.event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.registrations.EventValues.EventValueInfo;
import ch.njol.skript.variables.Variables;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.event.CustomEventManager.RegisteredEvent;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntax;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxCore;
import org.skriptlang.skript.bukkit.registration.BukkitRegistryKeys;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxOrigin;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class CustomEvent extends SkriptEvent implements CustomSyntax<BukkitSyntaxInfos.Event<?>> {

	private static final List<EventValueInfo<?, ?>> EVENT_VALUES_LIST
		= SkriptReflection.getEventValuesList(EventValues.TIME_NOW);

	private final CustomSyntaxCore core;
	private final String identifier;
	private final List<Class<?>> eventValueTypes;
	private final BukkitSyntaxInfos.Event<CustomEvent> info;
	private Trigger checkTrigger;
	private WeakReference<RegisteredEvent> registeredEventRef;

	public CustomEvent(
		String[] patterns,
		boolean hasParseSection,
		@Nullable Script script,
		Predicate<ParserInstance> usableInPredicate,
		String identifier,
		List<Class<?>> eventValueTypes
	) {
		this(
			new CustomSyntaxCore(
				patterns,
				hasParseSection,
				script,
				usableInPredicate
			),
			identifier,
			eventValueTypes,
			null
		);
	}

	public CustomEvent(
		CustomSyntaxCore core,
		String identifier,
		List<Class<?>> eventValueTypes,
		@Nullable Trigger checkTrigger
	) {
		this.core = core;
		this.identifier = identifier;
		this.eventValueTypes = eventValueTypes;
		this.checkTrigger = checkTrigger;
		this.info = BukkitSyntaxInfos.Event.builder(CustomEvent.class, identifier)
			.origin(SyntaxOrigin.of(SkriptMirror.getAddonInstance()))
			.supplier(this::copy)
			.addPatterns(core.patterns())
			.priority(core.priority())
			.build();
	}

	@Override
	public SyntaxRegistry.Key<BukkitSyntaxInfos.Event<?>> key() {
		return BukkitRegistryKeys.EVENT;
	}

	@Override
	public BukkitSyntaxInfos.Event<CustomEvent> info() {
		return info;
	}

	@Override
	public boolean register(SyntaxRegistry registry) {
		CustomSyntax.super.register(registry);
		if (CustomEventManager.isEventDefined(identifier)) {
			Skript.error("Custom event '" + identifier + "' is already registered.");
			return false;
		}

		registeredEventRef = CustomEventManager.defineCustomEvent(identifier);

		registerEventValues();

		return true;
	}

	private <T> void registerEventValues() {
		RegisteredEvent registeredEvent = registeredEventRef.get();
		if (registeredEvent == null)
			return;
		Class<? extends BukkitCustomEvent> eventClass = registeredEvent.eventClass();

		// Make sure more specific types come before more general types
		eventValueTypes.sort((a, b) -> {
			if (a == b)
				return 0;
			return a.isAssignableFrom(b) ? 1 : -1;
		});

		for (Class<?> eventValueType : eventValueTypes) {
			System.out.println("REGISTERING EVENT-VALUE: " + eventValueType.getSimpleName());
			EVENT_VALUES_LIST.add(createEventValueInfo(eventClass, eventValueType));
		}
	}

	@Override
	public void unregister(SyntaxRegistry registry) {
		CustomSyntax.super.unregister(registry);
		unregisterEventValues();
		CustomEventManager.unloadCustomEvent(identifier);
	}

	private void unregisterEventValues() {
		RegisteredEvent registeredEvent = registeredEventRef.get();
		if (registeredEvent == null)
			return;
		Class<? extends BukkitCustomEvent> eventClass = registeredEvent.eventClass();
		EVENT_VALUES_LIST.removeIf(info -> info.eventClass() == eventClass);
	}

	@Override
	public CustomEvent copy() {
		return new CustomEvent(core.copy(), identifier, eventValueTypes, checkTrigger);
	}

	@Override
	public Trigger parseTrigger() {
		return core.parseTrigger();
	}

	@Override
	public void parseTrigger(Trigger parseTrigger) {
		core.parseTrigger(parseTrigger);
	}

	public Trigger checkTrigger() {
		return checkTrigger;
	}

	public void checkTrigger(Trigger checkTrigger) {
		if (this.checkTrigger != null)
			throw new IllegalStateException("Check trigger is already set!");
		this.checkTrigger = checkTrigger;
	}

	@Override
	public boolean preInit() {
		return core.preInit() && super.preInit();
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
		return core.init(args, matchedPattern, parseResult);
	}

	@Override
	public boolean check(Event event) {
		if (checkTrigger == null)
			return true;

		EventCheckEvent checkEvent = new EventCheckEvent(
			event,
			core.expressions(),
			core.matchedPattern(),
			core.parseResult()
		);

		if (core.parseEvent() == null) {
			Trigger.walk(checkTrigger, checkEvent);
		} else {
			Variables.withLocalVariables(core.parseEvent(), checkEvent, () -> Trigger.walk(checkTrigger, checkEvent));
		}
		return checkEvent.isMarkedContinue();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return core.usedPattern();
	}

	private static <T> EventValueInfo<?, T> createEventValueInfo(
		Class<? extends BukkitCustomEvent> eventClass,
		Class<T> type
	) {
		return new EventValueInfo<>(
			eventClass,
			type,
			event -> event.getEventValue(type),
			null,
			null,
			EventValues.TIME_NOW
		);
	}

}
