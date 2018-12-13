package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

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
    if (objects != null) {
      ((ExpressionGetEvent) e).setOutput(objects.getAll(e));
    } else {
      ((ExpressionGetEvent) e).setOutput(new Object[0]);
    }
    return null;
  }

  @Override
  public String toString(Event e, boolean debug) {
    if (objects == null) {
      return "empty return";
    }
    return "return " + objects.toString(e, debug);
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    if (!ScriptLoader.isCurrentEvent(ExpressionGetEvent.class, ConstantGetEvent.class)) {
      Skript.error("Return may only be used in custom expression getters.", ErrorQuality.SEMANTIC_ERROR);
      return false;
    }

    if (!isDelayed.isTrue()) {
      Skript.error("Return may not be used if the code before it contains any delays.", ErrorQuality.SEMANTIC_ERROR);
    }

    objects = SkriptUtil.defendExpression(exprs[0]);

    return SkriptUtil.canInitSafely(objects);
  }
}
