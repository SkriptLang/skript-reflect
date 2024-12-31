package org.skriptlang.reflect.syntax.expression;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.HandlerList;

public class ConstantGetEvent extends ExpressionGetEvent {
	private final static HandlerList handlers = new HandlerList();

	public ConstantGetEvent(int matchedPattern, SkriptParser.ParseResult parseResult) {
		super(null, new Expression[0], matchedPattern, parseResult);
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
