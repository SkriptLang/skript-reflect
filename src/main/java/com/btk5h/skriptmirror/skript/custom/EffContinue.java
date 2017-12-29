package com.btk5h.skriptmirror.skript.custom;

import org.bukkit.event.Event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;

public class EffContinue extends Effect {
  static {
    Skript.registerEffect(EffContinue.class, "continue [[(with|using|returning)] %-objects%]");
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
        CustomExpression.ExpressionChangeEvent.class
    )) {
      Skript.error("Only custom syntax may be continued.",
          ErrorQuality.SEMANTIC_ERROR);
      return false;
    }

    objects = (Expression<Object>) exprs[0];

    return !(objects instanceof UnparsedLiteral);
  }
}
