package org.skriptlang.reflect.syntax.custom.condition;

import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.custom.shared.Continuable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;

public class ConditionCheckEvent extends CustomSyntaxEvent implements Continuable {

	private boolean markedContinue, negated;

	public ConditionCheckEvent(
		Event event,
		Condition self,
		Expression<?>[] expressions,
		int matchedPattern,
		ParseResult parseResult
	) {
		super(event, self, expressions, matchedPattern, parseResult);
	}

	public boolean isMarkedContinue() {
		return markedContinue;
	}

	@Override
	public void setContinue(boolean b) {
		markedContinue = b;
	}

	public boolean isNegated() {
		return negated;
	}

	public void markNegated() {
		negated = true;
	}

}
