package org.skriptlang.reflect.syntax.expression;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ExpressionGetEvent extends CustomSyntaxEvent {
	private final static HandlerList handlers = new HandlerList();
	private Object[] output;

	public ExpressionGetEvent(Event event, Expression<?>[] expressions, int matchedPattern,
							  SkriptParser.ParseResult parseResult) {
		super(event, expressions, matchedPattern, parseResult);
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Object[] getOutput() {
		return output;
	}

	public void setOutput(Object[] output) {
		this.output = output;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
