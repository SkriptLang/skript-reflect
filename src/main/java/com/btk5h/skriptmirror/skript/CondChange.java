package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;


public class CondChange extends Condition {

  private static final Patterns<ChangeMode> PATTERNS = new Patterns<>(new Object[][] {
      {"%classinfo% can be added to %expressions%", ChangeMode.ADD},
      {"%expressions% can be set to %classinfo%", ChangeMode.SET},
      {"%classinfo% can be removed from %expressions%", ChangeMode.REMOVE},
      {"all %classinfo% can be removed from %expressions%", ChangeMode.REMOVE_ALL},
      {"%expressions% can be deleted", ChangeMode.DELETE},
      {"%expressions% can be reset", ChangeMode.RESET}
  });

  static {
    Skript.registerCondition(CondChange.class, PATTERNS.getPatterns());
  }

  private ChangeMode desiredChangeMode;
  private boolean typeIsPlural;
  private Expression<ClassInfo<?>> desiredType;
  private Expression<Expression<?>> expressions;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    desiredChangeMode = PATTERNS.getInfo(matchedPattern);
    Expression<?> desiredType = null;
    switch (desiredChangeMode) {
      case ADD:
      case REMOVE:
      case REMOVE_ALL:
        desiredType = exprs[0];
        expressions = (Expression<Expression<?>>) exprs[1];
        break;
      case SET:
        expressions = (Expression<Expression<?>>) exprs[0];
        desiredType =  exprs[1];
        break;
      case RESET:
      case DELETE:
        expressions = (Expression<Expression<?>>) exprs[0];
    }
    if (desiredType instanceof UnparsedLiteral) {
      UnparsedLiteral unparsedDesiredType = (UnparsedLiteral) desiredType;
      typeIsPlural = Utils.getEnglishPlural(unparsedDesiredType.getData()).getSecond();
    }
    this.desiredType = (Expression<ClassInfo<?>>) desiredType;
    return true;
  }

  @Override
  public boolean check(Event event) {
    ClassInfo<?> desiredType = this.desiredType.getSingle(event);
    if (desiredType == null)
      return false;
    return expressions.check(event, expression -> acceptsChange(expression, desiredChangeMode, desiredType.getC(), typeIsPlural));
  }

  @Override
  public String toString(@Nullable Event event, boolean debug) {
    String expressionsString = expressions.toString(event, debug);
    String desiredTypesString = desiredType == null ? null : desiredType.toString(event, debug);
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

  private boolean acceptsChange(Expression<?> expression, ChangeMode desiredChangeMode, Class<?> desiredType, boolean typeIsPlural) {
    Class<?>[] acceptableTypes = expression.acceptChange(desiredChangeMode);
    //noinspection ConstantValue
    if (acceptableTypes != null) {
      for (Class<?> acceptableType : acceptableTypes) {
        if (acceptableType.isArray()
            && acceptableType.getComponentType().isAssignableFrom(desiredType)) {
          return true;
        } else if (!typeIsPlural && acceptableType.isAssignableFrom(desiredType))
          return true;
      }
      return false;
    }
    return desiredChangeMode == ChangeMode.DELETE || desiredChangeMode == ChangeMode.RESET;
  }

}
