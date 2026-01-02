package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleLiteral;
import org.bukkit.event.Event;

import java.util.Arrays;

public class SyntaxParseEvent extends CustomSyntaxEvent implements Continuable {

	private final Class<? extends Event>[] eventClasses;
	private boolean markedContinue;

	public SyntaxParseEvent(
		Expression<?>[] expressions,
		int matchedPattern,
		ParseResult parseResult,
		Class<? extends Event>[] eventClasses
	) {
		super(null, wrapRawExpressions(expressions), matchedPattern, parseResult);
		this.eventClasses = eventClasses;
	}

	public Class<? extends Event>[] eventClasses() {
		return eventClasses;
	}

	public boolean isMarkedContinue() {
		return markedContinue;
	}

	@Override
	public void setContinue(boolean b) {
		markedContinue = b;
	}

	private static Expression<?>[] wrapRawExpressions(Expression<?>[] expressions) {
		return Arrays.stream(expressions)
			.map(expr -> expr != null ? new SimpleLiteral<>(expr, false) : null)
			.toArray(Expression[]::new);
	}

}
