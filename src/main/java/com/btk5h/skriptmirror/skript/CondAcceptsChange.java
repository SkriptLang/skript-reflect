package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.ClassInfoReference;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;


public class CondAcceptsChange extends Condition {

  private static final Patterns<ChangeMode> PATTERNS = new Patterns<>(new Object[][] {
      {"%classinfo% can be added to %expressions%", ChangeMode.ADD},
      {"%classinfo% (can't|cannot) be added to %expressions%", ChangeMode.ADD},
      {"%expressions% can be set to %classinfo%", ChangeMode.SET},
      {"%expressions% (can't|cannot) be set to %classinfo%", ChangeMode.SET},
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
    Skript.registerCondition(CondAcceptsChange.class, PATTERNS.getPatterns());
  }

  private ChangeMode desiredChangeMode;
  private boolean desiredTypeIsPlural;
  private Expression<ClassInfoReference> desiredType;
  private Expression<Expression<?>> expressions;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
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
    if (desiredType != null) {
      this.desiredType = SkriptUtil.wrapClassInfoExpression((Expression<ClassInfo<?>>) desiredType);
    }
    return SkriptUtil.canInitSafely(desiredType);
  }

  @Override
  public boolean check(Event event) {
    if (desiredChangeMode == ChangeMode.DELETE || desiredChangeMode == ChangeMode.RESET)
      //noinspection ConstantValue
      return expressions.check(event, expressions -> expressions.acceptChange(desiredChangeMode) != null, isNegated());
    ClassInfoReference desiredType = this.desiredType.getSingle(event);
    if (desiredType == null)
      return false;
    return expressions.check(event, expression -> acceptsChange(expression, desiredChangeMode, desiredType), isNegated());
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

  private boolean acceptsChange(Expression<?> expression, ChangeMode desiredChangeMode, ClassInfoReference desiredType) {
    Class<?>[] acceptableTypes = expression.acceptChange(desiredChangeMode);
    //noinspection ConstantValue
    if (acceptableTypes != null) {
      for (Class<?> acceptableType : acceptableTypes) {
        if (acceptableType.isArray()
            && acceptableType.getComponentType().isAssignableFrom(desiredType.getClassInfo().getC())) {
          return true;
        } else if (desiredType.isPlural() && acceptableType.isAssignableFrom(desiredType.getClassInfo().getC()))
          return true;
      }
    }
    return false;
  }

}
