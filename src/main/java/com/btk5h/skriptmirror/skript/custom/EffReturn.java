package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;

public class EffReturn extends Effect {
  static {
    Skript.registerEffect(EffReturn.class, "return %objects%");
  }

  public Expression<Object> objects;

  @Override
  protected void execute(Event e) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected TriggerItem walk(Event e) {
    ((CustomExpression.ExpressionGetEvent) e).setOutput(objects.getAll(e));
    return null;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "return " + objects.toString(e, debug);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    if (!ScriptLoader.isCurrentEvent(CustomExpression.ExpressionGetEvent.class)) {
      Skript.error("Return may only be used in custom expression getters.", ErrorQuality.SEMANTIC_ERROR);
      return false;
    }

    objects = Util.defendExpression(exprs[0]);

    return Util.canInitSafely(objects);
  }
}
