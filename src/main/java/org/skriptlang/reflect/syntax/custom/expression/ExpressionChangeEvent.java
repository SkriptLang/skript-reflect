package org.skriptlang.reflect.syntax.custom.expression;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;

public class ExpressionChangeEvent extends CustomSyntaxEvent {

	private final Object[] delta;

	public ExpressionChangeEvent(
		Event event,
		Expression<?> self,
		Expression<?>[] expressions,
		int matchedPattern,
		ParseResult parseResult, Object[] delta
	) {
		super(event, self, expressions, matchedPattern, parseResult);
		this.delta = delta;
	}

	public Object[] delta() {
		return delta;
	}

}
