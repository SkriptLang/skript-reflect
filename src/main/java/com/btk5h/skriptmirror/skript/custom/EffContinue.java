package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

public class EffContinue extends Effect {
  static {
    Skript.registerEffect(EffContinue.class, "continue");
  }

  @Override
  protected void execute(Event e) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected TriggerItem walk(Event e) {
    if (e instanceof CustomEffect.EffectEvent) {
      if (((CustomEffect.EffectEvent) e).isSync()) {
        Skript.warning("Synchronous events should not be continued. " +
            "Call 'delay effect' to delay the effect's execution.");
      } else {
        TriggerItem.walk(((CustomEffect.EffectEvent) e).getNext(),
            ((CustomEffect.EffectEvent) e).getDirectEvent());
      }
    } else if (e instanceof CustomCondition.ConditionEvent) {
      ((CustomCondition.ConditionEvent) e).markContinue();
    }
    return null;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "continue";
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    if (!ScriptLoader.isCurrentEvent(CustomEffect.EffectEvent.class, CustomCondition.ConditionEvent.class)) {
      Skript.error("Return may only be used in custom effects and conditions.", ErrorQuality.SEMANTIC_ERROR);
      return false;
    }

    return true;
  }
}
