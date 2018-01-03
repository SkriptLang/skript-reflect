package com.btk5h.skriptmirror.skript.custom;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

public class ExprRawExpression extends SimpleExpression<Expression> {
  static {
    Skript.registerExpression(ExprRawExpression.class, Expression.class, ExpressionType.COMBINED,
        "[the] raw %objects%");
  }

  private Expression<?> expr;

  @Override
  protected Expression[] get(Event e) {
    return new Expression[] {expr};
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
  public String toString(Event e, boolean debug) {
    return "raw " + expr.toString(e, debug);
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    expr = exprs[0];
    if (expr instanceof CustomSyntaxExpression) {
      expr = expr.getSource();
    }
    return !(expr instanceof UnparsedLiteral);
  }
}
