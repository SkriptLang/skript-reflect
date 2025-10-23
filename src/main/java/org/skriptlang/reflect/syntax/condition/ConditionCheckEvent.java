package org.skriptlang.reflect.syntax.condition;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptmirror.skript.custom.Continuable;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ConditionCheckEvent extends CustomSyntaxEvent implements Continuable {

	private final static HandlerList handlers = new HandlerList();
	private boolean markedContinue;
	private boolean markedNegated = false;

	public ConditionCheckEvent(Event event, Expression<?>[] expressions, int matchedPattern,
							   SkriptParser.ParseResult parseResult) {
		super(event, expressions, matchedPattern, parseResult);
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public boolean isMarkedContinue() {
		return markedContinue;
	}

	public boolean isMarkedNegated() {
		return markedNegated;
	}

	@Override
	public void setContinue(boolean b) {
		markedContinue = b;
	}

	public void markNegated() {
		markedNegated = true;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
