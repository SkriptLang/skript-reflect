package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;

import java.util.Arrays;

public class ExprJavaTypeOf extends SimpleExpression<JavaType> {
  static {
    PropertyExpression.register(ExprJavaTypeOf.class, JavaType.class, "[java] class", "objects");
  }

  private Expression<Object> target;

  @Override
  protected JavaType[] get(Event e) {
    return Arrays.stream(target.getArray(e))
        .map(Util::getClass)
        .map(JavaType::new)
        .toArray(JavaType[]::new);
  }

  @Override
  public boolean isSingle() {
    return target.isSingle();
  }

  @Override
  public Class<? extends JavaType> getReturnType() {
    return JavaType.class;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "class of " + target.toString(e, debug);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    target = Util.defendExpression(exprs[0]);
    return Util.canInitSafely(target);
  }
}
