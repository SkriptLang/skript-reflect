package com.btk5h.skriptmirror.skript;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

public class ExprJavaError extends SimpleExpression<String> {
  static {
    Skript.registerExpression(ExprJavaError.class, String.class, ExpressionType.SIMPLE,
        "[the] [last] java error");
  }

  @Override
  protected String[] get(Event e) {
    Throwable lastError = ExprJavaCall.lastError;
    if (lastError == null) {
      if (ExprJavaCall.lastErrorMessage == null) {
        return new String[0];
      }
      return new String[]{ExprJavaCall.lastErrorMessage};
    }
    return new String[]{
        String.format("%s: %s",
            lastError.getClass().getSimpleName(),
            lastError.getMessage())
    };
  }

  @Override
  public boolean isSingle() {
    return true;
  }

  @Override
  public Class<? extends String> getReturnType() {
    return String.class;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "last java error";
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    return true;
  }
}
