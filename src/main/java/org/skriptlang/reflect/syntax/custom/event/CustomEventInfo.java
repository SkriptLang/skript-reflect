package org.skriptlang.reflect.syntax.custom.event;

import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import com.btk5h.skriptmirror.SkriptMirror;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.CustomSyntaxModule;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxInfo;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class CustomEventInfo extends CustomSyntaxInfo<CustomEvent> {

	private final BukkitSyntaxInfos.Event<CustomEvent> info;
	private final String identifier;
	private final List<Class<?>> eventValueTypes;
	private final List<EventValue<?, ?>> eventValues;
	private @Nullable Trigger checkTrigger;

	public CustomEventInfo(
		String[] patterns,
		boolean hasParseSection,
		@Nullable Script script,
		Predicate<ParserInstance> usableInPredicate,
		String identifier,
		List<Class<?>> eventValueTypes
	) {
		super(patterns, hasParseSection, script, usableInPredicate);
		this.identifier = identifier;
		this.eventValueTypes = eventValueTypes;
		this.eventValues = new ArrayList<>(eventValueTypes.size());
		this.info = BukkitSyntaxInfos.Event.builder(CustomEvent.class, identifier)
			.origin(CustomSyntaxModule.ORIGIN)
			.supplier(this::newInstance)
			.addEvent(BukkitCustomEvent.class)
			.addPatterns(patterns)
			.priority(priority())
			.build();
	}

	public String identifier() {
		return identifier;
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
		EventValueRegistry registry = SkriptMirror.getAddonInstance().registry(EventValueRegistry.class);
		for (Class<?> eventValueType : eventValueTypes) {
			EventValue<BukkitCustomEvent, ?> eventValue = createEventValue(eventValueType);
			eventValues.add(eventValue);
			registry.register(eventValue);
		}
	}

	@Override
	public boolean unregister(SyntaxRegistry registry) {
		registry.unregister(BukkitSyntaxInfos.Event.KEY, info);
		unregisterEventValues();
		CustomEventManager.unloadCustomEvent(identifier);
		return true;
	}

	private void unregisterEventValues() {
		EventValueRegistry registry = SkriptMirror.getAddonInstance().registry(EventValueRegistry.class);
		for (EventValue<?, ?> eventValue : eventValues)
			registry.unregister(eventValue);
		eventValues.clear();
	}

	@Override
	public CustomEvent newInstance() {
		return new CustomEvent(this);
	}

	private <T> EventValue<BukkitCustomEvent, T> createEventValue(Class<T> type) {
		return EventValue.builder(BukkitCustomEvent.class, type)
			.eventValidator(ignored -> CustomEventManager.isCurrentEvent(identifier())
				? EventValue.Validation.VALID
				: EventValue.Validation.INVALID)
			.getter(event -> event.getEventValue(type))
			.contextDependent()
			.build();
	}

}
