package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class ExprClassReference extends SimpleExpression<ObjectWrapper> {

  static {
    Skript.registerExpression(ExprClassReference.class, ObjectWrapper.class, ExpressionType.COMBINED,
      "%javatype%.class");
  }

  private Expression<? extends JavaType> javaTypeExpression;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    javaTypeExpression = (Expression<? extends JavaType>) exprs[0];

    return true;
  }

  @Nullable
  @Override
  protected ObjectWrapper[] get(Event e) {
    JavaType javaType = javaTypeExpression.getSingle(e);
    if (javaType == null)
      return null;
    Class<?> clazz = javaType.getJavaClass();

    return new ObjectWrapper[] {ObjectWrapper.create(clazz)};
  }

  @Override
  public boolean isSingle() {
    return true;
  }

  @Override
  public Class<? extends ObjectWrapper> getReturnType() {
    return ObjectWrapper.class;
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return javaTypeExpression.toString(e, debug) + ".class";
  }

}
