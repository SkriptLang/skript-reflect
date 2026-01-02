package org.skriptlang.reflect.syntax.custom.expression;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;

public class ExpressionGetEvent extends CustomSyntaxEvent {

	private Object[] output;

	public ExpressionGetEvent(
		Event event,
		Expression<?>[] expressions,
		int matchedPattern,
		ParseResult parseResult
	) {
		super(event, expressions, matchedPattern, parseResult);
	}

	public Object[] output() {
		return output;
	}

	public void output(Object[] output) {
		this.output = output;
	}

}
