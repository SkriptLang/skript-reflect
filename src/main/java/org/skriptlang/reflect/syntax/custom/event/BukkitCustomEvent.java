package org.skriptlang.reflect.syntax.custom.event;

import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BukkitCustomEvent extends Event implements Cancellable {

	private final String identifier;
	private final Map<Class<?>, Object> eventValueData = new HashMap<>();
	private final Map<String, Object> extraData = new HashMap<>();
	private boolean cancelled;

	public BukkitCustomEvent(String identifier) {
		this(identifier, !Bukkit.isPrimaryThread());
	}

	public BukkitCustomEvent(String identifier, boolean async) {
		super(async);
		this.identifier = identifier;
	}

	public String identifier() {
		return identifier;
	}

	public <T> T getEventValue(Class<T> type) {
		//noinspection unchecked
		return (T) eventValueData.get(type);
	}

	public <T> boolean setEventValue(Class<T> type, T value) {
		if (!type.isInstance(value))
			return false;
		eventValueData.put(type, value);
		return true;
	}

	@ApiStatus.Internal
	public <T> void setEventValues(Event event, Variable<?> variable, Consumer<String> errorHandler) {
		//noinspection unchecked
		Map<String, Object> raw = (Map<String, Object>) variable.getRaw(event);
		assert raw != null;
		for (Map.Entry<String, Object> entry : raw.entrySet()) {
			String typeName = entry.getKey();
			//noinspection unchecked
			Class<T> type = (Class<T>) Classes.getClassFromUserInput(typeName);
			if (type == null) {
				errorHandler.accept("Cannot set the event value for event '" + identifier + "' and type '" + typeName + "' as it doesn't exist");
				continue;
			}

			if (!(entry.getValue() instanceof Map<?,?> map)) {
				//noinspection unchecked
				T value = (T) entry.getValue();
				if (!setEventValue(type, value))
					errorHandler.accept(notOfType(value, type));
				continue;
			}

			//noinspection unchecked
			T single = (T) map.get(null);
			if (single != null && !setEventValue(type, single))
				errorHandler.accept(notOfType(single, type));

			//noinspection unchecked
			T plural = (T) map.entrySet().stream()
				.filter(e -> e.getKey() != null)
				.map(Map.Entry::getValue)
				.map(value -> value instanceof Map<?, ?> m ? m.get(null) : value)
				.filter(value -> {
					if (!type.isInstance(value)) {
						errorHandler.accept(notOfType(value, type));
						return false;
					}
					return true;
				})
				.toArray(size -> (Object[]) Array.newInstance(type, size));

			//noinspection unchecked
			setEventValue((Class<T>) type.arrayType(), plural);
		}

	}

	private static String notOfType(Object value, Class<?> type) {
		return value + " is " + SkriptParser.notOfType(type);
	}

	public Object getData(String key) {
		return extraData.get(key);
	}

	public void setData(String key, Object value) {
		extraData.put(key, value);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

}
