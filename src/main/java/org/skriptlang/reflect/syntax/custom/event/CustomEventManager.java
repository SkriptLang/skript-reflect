package org.skriptlang.reflect.syntax.custom.event;

import ch.njol.skript.lang.parser.ParserInstance;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class CustomEventManager {

	private static final Map<String, EventEntry> events = new HashMap<>();

	private CustomEventManager() {
		throw new UnsupportedOperationException();
	}

	public static void init() {
		ParserInstance.registerData(CustomEventData.class, CustomEventData::new);
	}

	public static boolean isEventDefined(String identifier) {
		return events.containsKey(identifier);
	}

	public static EventEntry getEvent(String identifier) {
		return events.get(identifier);
	}

	/**
	 * Generates and loads a {@link BukkitCustomEvent} subclass and factory class using the given identifier.
	 * @param identifier the event's identifier
	 * @return an instance of the generated factory class for the event
	 */
	public static EventEntry defineCustomEvent(String identifier) {
		if (isEventDefined(identifier))
			throw new IllegalArgumentException("Event '" + identifier + "' is already defined");
		EventEntry entry = new EventEntry(identifier);
		events.put(identifier, entry);
		return entry;
	}

	/**
	 * @param identifier the event's identifier
	 * @return true if the event was successfully unloaded
	 */
	public static boolean unloadCustomEvent(String identifier) {
		return events.remove(identifier) != null;
	}

	public static void setCurrentEvent(String identifier) {
		setCurrentEvent(ParserInstance.get(), identifier);
	}

	public static void setCurrentEvent(ParserInstance parser, String identifier) {
		parser.getData(CustomEventData.class).setCurrentEvent(identifier);
	}

	public static void deleteCurrentEvent() {
		CustomEventManager.deleteCurrentEvent(ParserInstance.get());
	}

	public static void deleteCurrentEvent(ParserInstance parser) {
		parser.getData(CustomEventData.class).setCurrentEvent(null);
	}

	public static @Nullable String getCurrentEvent() {
		return getCurrentEvent(ParserInstance.get());
	}

	public static @Nullable String getCurrentEvent(ParserInstance parser) {
		return parser.getData(CustomEventData.class).getCurrentEvent();
	}

	public static boolean isCurrentEvent(String identifier) {
		return isCurrentEvent(ParserInstance.get(), identifier);
	}

	public static boolean isCurrentEvent(ParserInstance parser, String identifier) {
		return parser.isCurrentEvent(BukkitCustomEvent.class)
			&& identifier.equals(parser.getData(CustomEventData.class).getCurrentEvent());
	}

	public record EventEntry(String identifier) {

		public BukkitCustomEvent newInstance() {
			return new BukkitCustomEvent(identifier);
		}

	}

	public static class CustomEventData extends ParserInstance.Data {

		private @Nullable String currentEvent;

		public CustomEventData(ParserInstance parserInstance) {
			super(parserInstance);
		}

		public void setCurrentEvent(@Nullable String identifier) {
			currentEvent = identifier;
		}

		public @Nullable String getCurrentEvent() {
			return currentEvent;
		}

	}

}
