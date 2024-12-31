package org.skriptlang.reflect.syntax.condition.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;

public class EffNegateCondition extends Effect {
	static {
		Skript.registerEffect(EffNegateCondition.class, "negate [the] [current] condition");
	}

	@Override
	protected void execute(Event e) {
		((ConditionCheckEvent) e).markNegated();
	}

	@Override
	public String toString(Event e, boolean debug) {
		return "negate condition";
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		if (!getParser().isCurrentEvent(ConditionCheckEvent.class)) {
			Skript.error("The effect 'negate condition' may only be used in a custom condition.",
				ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		return true;
	}
}
