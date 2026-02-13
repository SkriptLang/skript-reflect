package org.skriptlang.reflect.syntax.custom.event;

import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.registrations.EventValues;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.event.CustomEventManager.RegisteredEvent;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxInfo;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Predicate;

public class CustomEventInfo extends CustomSyntaxInfo<CustomEvent> {

	private static final List<EventValues.EventValueInfo<?, ?>> EVENT_VALUES_LIST
		= SkriptReflection.getEventValuesList(EventValues.TIME_NOW);

	private final BukkitSyntaxInfos.Event<CustomEvent> info;
	private final String identifier;
	private final List<Class<?>> eventValueTypes;
	private final WeakReference<RegisteredEvent> registeredEventRef;
	private @Nullable Trigger checkTrigger;

	public CustomEventInfo(
		String[] patterns,
		boolean hasParseSection,
		@Nullable Script script,
		Predicate<ParserInstance> usableInPredicate,
		String identifier,
		List<Class<?>> eventValueTypes,
		RegisteredEvent registeredEvent
	) {
		super(patterns, hasParseSection, script, usableInPredicate);
		this.identifier = identifier;
		this.eventValueTypes = eventValueTypes;
		this.registeredEventRef = new WeakReference<>(registeredEvent);
		this.info = BukkitSyntaxInfos.Event.builder(CustomEvent.class, identifier)
			.supplier(this::newInstance)
			.addEvent(registeredEvent.eventClass())
			.addPatterns(patterns)
			.priority(priority())
			.build();
	}

	public String identifier() {
		return identifier;
	}

	public RegisteredEvent registeredEvent() {
		return registeredEventRef.get();
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
	public boolean register(SyntaxRegistry registry) {
		registry.register(BukkitSyntaxInfos.Event.KEY, info);

		registerEventValues();

		return true;
	}

	private void registerEventValues() {
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

		for (Class<?> eventValueType : eventValueTypes)
			EVENT_VALUES_LIST.add(createEventValueInfo(eventClass, eventValueType));
	}

	@Override
	public boolean unregister(SyntaxRegistry registry) {
		registry.unregister(BukkitSyntaxInfos.Event.KEY, info);
		unregisterEventValues();
		CustomEventManager.unloadCustomEvent(identifier);
		return true;
	}

	private void unregisterEventValues() {
		RegisteredEvent registeredEvent = registeredEventRef.get();
		if (registeredEvent == null)
			return;
		Class<? extends BukkitCustomEvent> eventClass = registeredEvent.eventClass();
		EVENT_VALUES_LIST.removeIf(info -> info.eventClass() == eventClass);
	}

	@Override
	public CustomEvent newInstance() {
		return new CustomEvent(this);
	}

	private static <T> EventValues.EventValueInfo<?, T> createEventValueInfo(
		Class<? extends BukkitCustomEvent> eventClass,
		Class<T> type
	) {
		return new EventValues.EventValueInfo<>(
			eventClass,
			type,
			event -> event.getEventValue(type),
			null,
			null,
			EventValues.TIME_NOW
		);
	}

}
