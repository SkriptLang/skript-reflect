package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;

public class ExprEventData extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprEventData.class, Object.class, ExpressionType.COMBINED, "[extra] [event(-| )]data %string%");
	}

	private Expression<String> dataIndex;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (!getParser().isCurrentEvent(BukkitCustomEvent.class, EventTriggerEvent.class)) {
			Skript.error("The event data expression can only be used in a custom event");
			return false;
		}
		dataIndex = (Expression<String>) exprs[0];
		return true;
	}

	@Nullable
	@Override
	protected Object[] get(Event e) {
		String key = dataIndex.getSingle(e);
		if (key == null)
			return null;

		BukkitCustomEvent bukkitCustomEvent;
		if (e instanceof BukkitCustomEvent) {
			bukkitCustomEvent = (BukkitCustomEvent) e;
		} else {
			bukkitCustomEvent = (BukkitCustomEvent) ((EventTriggerEvent) e).getDirectEvent();
		}

		Object data = bukkitCustomEvent.getData(key);
		return new Object[] {data};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<?> getReturnType() {
		return Object.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "event data";
	}

}
