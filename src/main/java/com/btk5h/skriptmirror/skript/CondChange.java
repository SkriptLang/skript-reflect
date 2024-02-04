package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;


public class CondChange extends Condition {

  private static final Patterns<ChangeMode> PATTERNS = new Patterns<>(new Object[][] {
      {"%classinfos% can be added to %expressions%", ChangeMode.ADD},
      {"%expressions% can be set to %classinfos%", ChangeMode.SET},
      {"%classinfos% can be removed from %expressions%", ChangeMode.REMOVE},
      {"all %classinfos% can be removed from %expressions%", ChangeMode.REMOVE_ALL},
      {"%expressions% can be deleted", ChangeMode.DELETE},
      {"%expressions% can be reset", ChangeMode.RESET}
  });

  static {
    Skript.registerCondition(CondChange.class, PATTERNS.getPatterns());
  }

  private ChangeMode desiredChangeMode;
  private Expression<ClassInfo<?>> desiredTypes;
  private Expression<Expression<?>> expressions;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    desiredChangeMode = PATTERNS.getInfo(matchedPattern);
    switch (desiredChangeMode) {
      case ADD:
      case REMOVE:
      case REMOVE_ALL:
        desiredTypes = (Expression<ClassInfo<?>>) exprs[0];
        expressions = (Expression<Expression<?>>) exprs[1];
        break;
      case SET:
        expressions = (Expression<Expression<?>>) exprs[0];
        desiredTypes = (Expression<ClassInfo<?>>) exprs[1];
        break;
      case RESET:
      case DELETE:
        expressions = (Expression<Expression<?>>) exprs[0];
    }
    return true;
  }

  @Override
  public boolean check(Event event) {
    if (desiredChangeMode == ChangeMode.DELETE || desiredChangeMode == ChangeMode.RESET) {
      //noinspection ConstantValue
      return expressions.check(event, expr -> expr.acceptChange(desiredChangeMode) != null);
    }
    return expressions.check(event, expression -> acceptsChange(expression, desiredChangeMode, getDesiredTypes(event)));
  }

  @Override
  public String toString(@Nullable Event event, boolean debug) {
    String expressionsString = expressions.toString(event, debug);
    String desiredTypesString = desiredTypes == null ? null : desiredTypes.toString(event, debug);
    switch (desiredChangeMode) {
      case ADD:
        return desiredTypesString + " can be added to " + expressionsString;
      case SET:
        return expressionsString + " can be set to " + desiredTypesString;
      case RESET:
        return expressionsString + " can be reset";
      case DELETE:
        return expressionsString + " can be deleted";
      case REMOVE:
        return desiredTypesString + " can be removed from " + expressionsString;
      case REMOVE_ALL:
        return "all " + desiredTypesString + " can be removed from " + expressionsString;
      default:
        throw new IllegalStateException();
    }
  }

  private boolean acceptsChange(Expression<?> expression, ChangeMode desiredChangeMode, Collection<Class<?>> desiredTypes) {
    Class<?>[] acceptableTypes = expression.acceptChange(desiredChangeMode);
    for (Class<?> desiredType : desiredTypes) {
      boolean multipleDesired = desiredType.isArray();
      if (multipleDesired)
        desiredType = desiredType.getComponentType();
      for (Class<?> acceptableType : acceptableTypes) {
        if (acceptableType.isArray()
            && acceptableType.getComponentType().isAssignableFrom(desiredType)) {
          return true;
        } else if (!multipleDesired && acceptableType.isAssignableFrom(desiredType))
          return true;
      }
    }
    return false;
  }

  private Set<Class<?>> getDesiredTypes(Event event) {
    return desiredTypes.stream(event).map(ClassInfo::getC).collect(Collectors.toSet());
  }

}
