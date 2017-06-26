package com.btk5h.skriptmirror.skript;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

public class ExprCollect extends SimpleExpression<Object> {
  static {
    Skript.registerExpression(ExprCollect.class, Object.class, ExpressionType.COMBINED,
        "\\[%objects%\\]");
  }

  private Expression<Object> objects;

  @Override
  protected Object[] get(Event e) {
    return new Object[]{objects.getArray(e)};
  }

  @Override
  public boolean isSingle() {
    return true;
  }

  @Override
  public Class<?> getReturnType() {
    return Object.class;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "array of " + objects.toString(e, debug);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    objects = (Expression<Object>) exprs[0];
    return !(objects instanceof UnparsedLiteral);
  }
}
