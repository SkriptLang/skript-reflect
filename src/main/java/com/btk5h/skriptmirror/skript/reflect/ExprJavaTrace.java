package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

import java.util.Arrays;

public class ExprJavaTrace extends SimpleExpression<String> {
  static {
    Skript.registerExpression(ExprJavaTrace.class, String.class, ExpressionType.SIMPLE,
        "[the] [last] java [stack] trace");
  }

  @Override
  protected String[] get(Event e) {
    if (ExprJavaCall.lastError == null) {
      return new String[0];
    }
    return Arrays.stream(ExprJavaCall.lastError.getStackTrace())
        .map(StackTraceElement::toString)
        .toArray(String[]::new);
  }

  @Override
  public boolean isSingle() {
    return false;
  }

  @Override
  public Class<? extends String> getReturnType() {
    return String.class;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "last java stack trace";
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    return true;
  }
}
