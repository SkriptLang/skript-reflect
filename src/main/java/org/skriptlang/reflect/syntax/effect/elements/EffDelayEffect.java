package org.skriptlang.reflect.syntax.effect.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;

public class EffDelayEffect extends Effect {
	static {
		Skript.registerEffect(EffDelayEffect.class, "delay [the] [current] effect");
	}

	@Override
	protected void execute(Event e) {
		((EffectTriggerEvent) e).setSync(false);
	}

	@Override
	public String toString(Event e, boolean debug) {
		return "delay effect";
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		if (!getParser().isCurrentEvent(EffectTriggerEvent.class)) {
			Skript.error("The effect 'delay effect' may only be used in a custom effect.", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		return true;
	}
}
