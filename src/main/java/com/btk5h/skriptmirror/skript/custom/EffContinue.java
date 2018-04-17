package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

public class EffContinue extends Effect {
  static {
    Skript.registerEffect(EffContinue.class, "continue [(if|when) (%-boolean%|<.+>)]");
  }

  private Expression<Boolean> condition;
  private Condition skriptCondition;

  @Override
  protected void execute(Event e) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected TriggerItem walk(Event e) {
    if (skriptCondition != null && !skriptCondition.check(e)
        || condition != null && condition.getSingle(e) != Boolean.TRUE) {
      return null;
    }

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

    condition = (Expression<Boolean>) exprs[0];

    if (parseResult.regexes.size() > 0) {
      String group = parseResult.regexes.get(0).group();
      skriptCondition = Condition.parse(group, "Can't understand this condition: " + group);

      return skriptCondition != null;
    }

    return true;
  }
}
