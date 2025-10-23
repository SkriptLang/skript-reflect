package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.bukkit.event.Event;

public class EffContinue extends Effect {

	static {
		Skript.registerEffect(EffContinue.class, "continue");
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		if (!(getParser().isCurrentEvent(EffectTriggerEvent.class)
			|| CollectionUtils.containsAnySuperclass(new Class[]{Continuable.class}, getParser().getCurrentEvents()))) {
			Skript.error("Continue may only be used in loops, custom effects, custom syntax parse sections and custom conditions", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}

		return true;
	}

	@Override
	protected void execute(Event e) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected TriggerItem walk(Event e) {
		if (e instanceof EffectTriggerEvent) {
			EffectTriggerEvent effectTriggerEvent = (EffectTriggerEvent) e;
			if (effectTriggerEvent.isSync()) {
				Skript.warning("Synchronous events should not be continued. Call 'delay effect' to delay the effect's execution.");
			} else {
				effectTriggerEvent.setContinued();
				TriggerItem.walk(effectTriggerEvent.getNext(), effectTriggerEvent.getDirectEvent());
			}
		} else if (e instanceof Continuable) {
			((Continuable) e).markContinue();
		}
		return null;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return "continue";
	}
}
