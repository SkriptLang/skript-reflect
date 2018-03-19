package com.btk5h.skriptmirror.skript;

import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.Null;
import com.btk5h.skriptmirror.Util;

import org.bukkit.event.Event;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Converters;
import ch.njol.util.Kleenean;

public class ExprCast extends SimpleExpression<Object> {
  static {
    Skript.registerExpression(ExprCast.class, Object.class, ExpressionType.COMBINED,
        "%objects% (as|converted to) %javatype%");
  }

  private Expression<Object> source;
  private Expression<JavaType> type;

  @Override
  protected Object[] get(Event e) {
    JavaType t = type.getSingle(e);

    if (t == null) {
      return null;
    }

    return convertArray(source.getArray(e), t.getJavaClass());
  }

  private static Object[] convertArray(Object[] o, Class<?> to) {
    return Arrays.stream(o)
        .map(converter(to))
        .filter(Objects::nonNull)
        .toArray(Object[]::new);
  }

  private static <F> Function<F, Object> converter(Class<?> to) {
    return o -> o instanceof Null ? o : Converters.convert(o, to);
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
    source = Util.defendExpression(exprs[0]);
    type = Util.defendExpression(exprs[1]);
    return Util.canInitSafely(source);
  }
}
