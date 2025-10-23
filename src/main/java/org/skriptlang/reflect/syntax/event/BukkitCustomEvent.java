package org.skriptlang.reflect.syntax.event;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.HashMap;
import java.util.Map;

/**
 * The Bukkit event class which is used for custom events. Event values can be accessed with
 * {@link #getEventValue(ClassInfo)} or {@link #getEventValue(String)}.
 *
 * If extra data is needed, {@link #setData(String, Object)} and {@link #getData(String)} can be used.
 */
public class BukkitCustomEvent extends Event implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();
	private final String name;
	private final Map<ClassInfo<?>, Object> eventValueMap;
	private final Map<String, Object> dataMap;
	private boolean isCancelled = false;

	public BukkitCustomEvent(String name) {
		this(name, !Bukkit.isPrimaryThread());
	}

	public BukkitCustomEvent(String name, boolean isAsync) {
		super(isAsync);
		this.name = name;
		this.eventValueMap = new HashMap<>();
		this.dataMap = new HashMap<>();
	}

	public String getName() {
		return this.name;
	}

	public void setEventValue(ClassInfo<?> classInfo, Object value) {
		if (classInfo != null && classInfo.getC().isInstance(value))
			this.eventValueMap.put(classInfo, value);
	}

	public void setEventValue(String type, Object value) {
		setEventValue(Classes.getClassInfoFromUserInput(type), value);
	}

	public Object getEventValue(ClassInfo<?> classInfo) {
		Class<?> clazz = classInfo.getC();
		ClassInfo<?> chosenClassInfo = null;
		for (ClassInfo<?> classInfoKey : this.eventValueMap.keySet()) {
			if (clazz.isAssignableFrom(classInfoKey.getC())) {
				chosenClassInfo = classInfoKey;
				break;
			}
		}
		return chosenClassInfo == null ? null : getExactEventValue(chosenClassInfo);
	}

	public Object getExactEventValue(ClassInfo<?> classInfo) {
		return this.eventValueMap.get(classInfo);
	}

	public Object getEventValue(String type) {
		ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(type);
		return classInfo == null ? null : getEventValue(classInfo);
	}

	public void setData(String key, Object value) {
		this.dataMap.put(key, value);
	}

	public Object getData(String key) {
		return this.dataMap.get(key);
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		isCancelled = cancel;
	}
}
