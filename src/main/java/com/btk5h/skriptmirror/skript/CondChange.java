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
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;


public class CondChange extends Condition {

  private static final Patterns<ChangeMode> PATTERNS = new Patterns<>(new Object[][] {
      {"%classinfo% can be added to %expressions%", ChangeMode.ADD},
      {"%classinfo% (can't|cannot) be added to %expressions%", ChangeMode.ADD},
      {"%expressions% can be set to %*classinfo%", ChangeMode.SET},
      {"%expressions% (can't|cannot) be set to %*classinfo%", ChangeMode.SET},
      {"%classinfo% can be removed from %expressions%", ChangeMode.REMOVE},
      {"%classinfo% (can't|cannot) be removed from %expressions%", ChangeMode.REMOVE},
      {"all %classinfo% can be removed from %expressions%", ChangeMode.REMOVE_ALL},
      {"all %classinfo% (can't|cannot) be removed from %expressions%", ChangeMode.REMOVE_ALL},
      {"%expressions% can be deleted", ChangeMode.DELETE},
      {"%expressions% (can't|cannot) be deleted", ChangeMode.DELETE},
      {"%expressions% can be reset", ChangeMode.RESET},
      {"%expressions% (can't|cannot) be reset", ChangeMode.RESET}
  });

  static {
    Skript.registerCondition(CondChange.class, PATTERNS.getPatterns());
  }

  private ChangeMode desiredChangeMode;
  private boolean typeIsPlural;
  private Expression<ClassInfo<?>> desiredType;
  private Expression<Expression<?>> expressions;
  private String rawForm;
  private boolean wasUnparsed;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    rawForm = parseResult.expr;
    setNegated((matchedPattern % 2) != 0);
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
      wasUnparsed = true;
      UnparsedLiteral unparsedDesiredType = (UnparsedLiteral) desiredType;
      desiredType = unparsedDesiredType.getConvertedExpression(ClassInfo.class);
      if (desiredType == null)
        return false;
      typeIsPlural = Utils.getEnglishPlural(unparsedDesiredType.getData()).getSecond();
    }
    this.desiredType = (Expression<ClassInfo<?>>) desiredType;
    return true;
  }

  @Override
  public boolean check(Event event) {
    if (desiredChangeMode == ChangeMode.DELETE || desiredChangeMode == ChangeMode.RESET)
      //noinspection ConstantValue
      return expressions.check(event, expressions -> expressions.acceptChange(desiredChangeMode) != null, isNegated());
    ClassInfo<?> desiredType = this.desiredType.getSingle(event);
    if (desiredType == null)
      return false;
    Bukkit.getConsoleSender().sendMessage(toString(event, true));
    Bukkit.getConsoleSender().sendMessage("- raw: " + rawForm);
    Bukkit.getConsoleSender().sendMessage("- was unparsed: " + wasUnparsed);
    Bukkit.getConsoleSender().sendMessage("- plural: " + typeIsPlural);
    Bukkit.getConsoleSender().sendMessage("- change mode: " + desiredChangeMode.name());
    Bukkit.getConsoleSender().sendMessage("- desired type: " + desiredType.getC().getCanonicalName());
    return expressions.check(event, expression -> acceptsChange(expression, desiredChangeMode, desiredType.getC(), typeIsPlural), isNegated());
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
    }
    return false;
  }

}
