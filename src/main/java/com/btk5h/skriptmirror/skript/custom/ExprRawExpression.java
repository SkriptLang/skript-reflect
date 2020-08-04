package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.WrappedEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

public class ExprRawExpression extends SimpleExpression<Expression> {
  static {
    Skript.registerExpression(ExprRawExpression.class, Expression.class, ExpressionType.COMBINED,
        "[the] raw %objects%");
  }

  private Expression<?> expr;

  @Override
  protected Expression[] get(Event e) {
    Expression<?> expr = this.expr;
    if (expr instanceof ExprExpression && e instanceof CustomSyntaxEvent) {
      expr = ((ExprExpression) expr).getExpression(e).getSource();
    }
    return new Expression[]{expr};
  }

  @Override
  public boolean isSingle() {
    return true;
  }

  @Override
  public Class<? extends Expression> getReturnType() {
    return Expression.class;
  }

  @Override
  public Class<?>[] acceptChange(Changer.ChangeMode changeMode) {
    return expr instanceof ExprExpression ? new Class[] {Object[].class} : null;
  }

  @Override
  public void change(Event event, Object[] delta, Changer.ChangeMode changeMode) {
    if (!(expr instanceof ExprExpression && event instanceof CustomSyntaxEvent))
      return;

    Expression<?> expr = ((ExprExpression<?>) this.expr).getExpression(event).getSource();

    event = ((WrappedEvent) event).getEvent();
    expr.change(event, delta, changeMode);
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "raw " + expr.toString(e, debug);
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    expr = SkriptUtil.defendExpression(exprs[0]);
    return SkriptUtil.canInitSafely(expr);
  }
}
