package com.btk5h.skriptmirror.skript.custom;

import com.btk5h.skriptmirror.Util;

import org.bukkit.event.Event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;

public class EffReturn extends Effect {
  static {
    Skript.registerEffect(EffReturn.class, "return [%-objects%]");
  }

  public Expression<Object> objects;

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
    } else if (e instanceof CustomExpression.ExpressionGetEvent) {
      if (objects != null) {
        ((CustomExpression.ExpressionGetEvent) e).setOutput(objects.getAll(e));
      } else {
        ((CustomExpression.ExpressionGetEvent) e).setOutput(new Object[0]);
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
    if (!ScriptLoader.isCurrentEvent(
        CustomEffect.EffectEvent.class,
        CustomExpression.ExpressionGetEvent.class,
        CustomExpression.ExpressionChangeEvent.class,
        CustomCondition.ConditionEvent.class

    )) {
      Skript.error("Return may only be used in custom syntax.", ErrorQuality.SEMANTIC_ERROR);
      return false;
    }

    objects = Util.defendExpression(exprs[0]);

    return Util.canInitSafely(objects);
  }
}
