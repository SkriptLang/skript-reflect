package org.skriptlang.reflect.syntax.custom.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.eclipse.sisu.space.asm.*;

import java.util.HashMap;
import java.util.Map;

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
