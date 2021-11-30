package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.reflect.sections.SectionEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

public class EffReturn extends Effect {
  static {
    Skript.registerEffect(EffReturn.class, "return [%-objects%]");
  }

  public Expression<?> objects;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
    Expression<?> expr = SkriptUtil.defendExpression(exprs[0]);
    if (!SkriptUtil.canInitSafely(expr)) {
      Skript.error("Can't understand this expression: " + expr);
      return false;
    }

    if (!getParser().isCurrentEvent(ExpressionGetEvent.class, ConstantGetEvent.class, SectionEvent.class)) {
      // No error message so it'll default to Skript's EffReturn
      return false;
    }

    SkriptEvent skriptEvent = getParser().getCurrentSkriptEvent();
    if (expr != null && skriptEvent instanceof CustomExpressionSection) {
      CustomExpressionSection customExpressionSection = (CustomExpressionSection) skriptEvent;
      ExpressionSyntaxInfo which = customExpressionSection.getFirstWhich();
      Class<?> returnType = CustomExpressionSection.returnTypes.get(which);
      if (returnType != null) {
        Expression<?> newExpr = expr.getConvertedExpression(returnType);
        if (newExpr == null) {
          Skript.error(expr + " is not " + Classes.getSuperClassInfo(returnType).getName().withIndefiniteArticle());
          return false;
        }
        expr = newExpr;
      }
    }

    if (!isDelayed.isFalse()) {
      Skript.error("Return may not be used if the code before it contains any delays", ErrorQuality.SEMANTIC_ERROR);
      return false;
    }

    objects = expr;

    return true;
  }

  @Override
  protected TriggerItem walk(Event e) {
    if (e instanceof SectionEvent) {
      ((SectionEvent) e).setOutput(objects == null ? new Object[0] : objects.getArray(e));
    } else {
      ((ExpressionGetEvent) e).setOutput(objects == null ? new Object[0] : objects.getArray(e));
    }
    return null;
  }

  @Override
  protected void execute(Event e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString(Event e, boolean debug) {
    if (objects == null) {
      return "return";
    }
    return "return " + objects.toString(e, debug);
  }

}
