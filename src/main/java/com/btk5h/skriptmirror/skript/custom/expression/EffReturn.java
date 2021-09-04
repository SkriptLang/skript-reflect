package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.reflect.sections.SectionEvent;
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
    if (e instanceof SectionEvent) {
      ((SectionEvent) e).setOutput(objects == null ? new Object[0] : objects.getAll(e));
      return null;
    }

    ((ExpressionGetEvent) e).setOutput(objects == null ? new Object[0] : objects.getAll(e));
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
    if (!getParser().isCurrentEvent(ExpressionGetEvent.class, ConstantGetEvent.class, SectionEvent.class)) {
      // No error message because it'll default to Skript's EffReturn
      return false;
    }

    if (!isDelayed.isFalse()) {
      Skript.error("Return may not be used if the code before it contains any delays", ErrorQuality.SEMANTIC_ERROR);
      return false;
    }

    objects = SkriptUtil.defendExpression(exprs[0]);

    return SkriptUtil.canInitSafely(objects);
  }
}
