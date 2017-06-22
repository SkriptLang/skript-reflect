package com.btk5h.skriptmirror.skript;

import com.btk5h.skriptmirror.Util;

import org.bukkit.event.Event;

import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

public class ExprMembers extends SimpleExpression<String> {
  static {
    PropertyExpression.register(ExprMembers.class, String.class,
        "(0¦fields|1¦methods|2¦constructors)", "objects");
  }

  private Expression<Object> target;
  private Function<Class<?>, Stream<? extends Member>> mapper;

  @Override
  protected String[] get(Event e) {
    return Arrays.stream(target.getArray(e))
        .map(Util::toClass)
        .flatMap(mapper)
        .map(Util::toGenericString)
        .distinct()
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
    return "fields/methods/constructors";
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    target = (Expression<Object>) exprs[0];

    switch (matchedPattern) {
      case 0:
        mapper = Util::fields;
        break;
      case 1:
        mapper = Util::methods;
        break;
      case 2:
        mapper = Util::constructor;
        break;
    }
    return !(target instanceof UnparsedLiteral);
  }
}
