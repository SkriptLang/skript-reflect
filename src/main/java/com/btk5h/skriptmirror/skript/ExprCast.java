package com.btk5h.skriptmirror.skript;

import com.btk5h.skriptmirror.JavaType;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

public class ExprCast extends SimpleExpression<Object> {
  static {
    Skript.registerExpression(ExprCast.class, Object.class, ExpressionType.COMBINED,
        "%objects% (as|converted to) %javatype/classinfo%");
  }

  private Expression<Object> source;
  private Expression<Object> type;

  @Override
  protected Object[] get(Event e) {
    Object t = type.getSingle(e);
    if (t == null) {
      return null;
    }

    Class convertTo;
    if (t instanceof ClassInfo) {
      convertTo = ((ClassInfo) t).getC();
    } else if (t instanceof JavaType) {
      convertTo = ((JavaType) t).getJavaClass();
    } else {
      throw new IllegalStateException();
    }

    Expression converted = source.getConvertedExpression(convertTo);
    if (converted == null) {
      return null;
    }

    return converted.getArray(e);
  }

  @Override
  public boolean isSingle() {
    return source.isSingle();
  }

  @Override
  public Class<?> getReturnType() {
    return source.getReturnType();
  }

  @Override
  public String toString(Event e, boolean debug) {
    return source.toString(e, debug);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    source = (Expression<Object>) exprs[0];
    type = (Expression<Object>) exprs[1];
    return !(source instanceof UnparsedLiteral);
  }
}
