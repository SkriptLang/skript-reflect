package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;

import java.util.*;


public class CustomExpression<T> implements Expression<T> {
  private SyntaxInfo which;
  private Expression<?>[] exprs;
  private SkriptParser.ParseResult parseResult;

  private final CustomExpression<?> source;
  private final Class<? extends T>[] types;
  private final Class<T> superType;

  @SuppressWarnings("unchecked")
  public CustomExpression() {
    this(null, (Class<? extends T>) Object.class);
  }

  @SuppressWarnings("unchecked")
  @SafeVarargs
  private CustomExpression(CustomExpression<?> source, Class<? extends T>... types) {
    this.source = source;

    if (source != null) {
      this.which = source.which;
      this.exprs = source.exprs;
      this.parseResult = source.parseResult;
    }

    this.types = types;
    this.superType = (Class<T>) Utils.getSuperType(types);
  }

  @Override
  public T getSingle(Event e) {
    T[] all = getAll(e);
    return all.length == 0 ? null : all[0];
  }

  @Override
  public T[] getArray(Event e) {
    return getAll(e);
  }

  @Override
  public T[] getAll(Event e) {
    Trigger getter = CustomExpressionSection.expressionHandlers.get(which);

    if (getter == null) {
      Skript.error(
          String.format("The custom expression '%s' no longer has a get handler.",
              which.getPattern())
      );
      return Util.newArray(superType, 0);
    }

    if (which.isProperty()) {
      return getByProperty(e, getter);
    }

    return getByStandard(e, getter);
  }

  private T[] getByStandard(Event e, Trigger getter) {
    ExpressionGetEvent expressionEvent = new ExpressionGetEvent(e, exprs, parseResult);
    getter.execute(expressionEvent);
    if (expressionEvent.getOutput() == null) {
      Skript.error(
          String.format("The get handler for '%s' did not return.", which.getPattern())
      );
      return Util.newArray(superType, 0);
    }

    return Converters.convertArray(expressionEvent.getOutput(), types, superType);
  }

  private T[] getByProperty(Event e, Trigger getter) {
    List<T> output = new ArrayList<>();
    for (Object o : exprs[0].getArray(e)) {
      Expression<?>[] localExprs = Arrays.copyOf(exprs, exprs.length);
      localExprs[0] = new SimpleLiteral<>(o, false);

      ExpressionGetEvent expressionEvent = new ExpressionGetEvent(e, localExprs, parseResult);
      getter.execute(expressionEvent);

      Object[] exprOutput = expressionEvent.getOutput();
      if (exprOutput == null) {
        Skript.error(
            String.format("The get handler for '%s' did not return.", which.getPattern())
        );
        return Util.newArray(superType, 0);
      }

      if (exprOutput.length > 1) {
        Skript.error(
            String.format("The get handler for '%s' returned more than one value.", which.getPattern())
        );
        return Util.newArray(superType, 0);
      }

      if (exprOutput.length == 1) {
        output.add(Converters.convert(exprOutput[0], superType));
      }
    }

    return output.toArray(Util.newArray(superType, 0));
  }

  @Override
  public boolean isSingle() {
    return !which.isAlwaysPlural() &&
        Arrays.stream(which.getInheritedSingles())
            .mapToObj(i -> exprs[i])
            .filter(Objects::nonNull)
            .allMatch(Expression::isSingle);
  }

  @Override
  public boolean check(Event e, Checker<? super T> c, boolean negated) {
    return SimpleExpression.check(getAll(e), c, negated, getAnd());
  }

  @Override
  public boolean check(Event e, Checker<? super T> c) {
    return SimpleExpression.check(getAll(e), c, false, getAnd());
  }

  @Override
  public <R> Expression<? extends R> getConvertedExpression(Class<R>[] to) {
    if (CustomExpressionSection.returnTypes.containsKey(which)
        && !Converters.converterExists(CustomExpressionSection.returnTypes.get(which), to)) {
      return null;
    }

    return new CustomExpression<>(this, to);
  }

  @Override
  public Class<T> getReturnType() {
    return superType;
  }

  @Override
  public boolean getAnd() {
    return true;
  }

  @Override
  public boolean setTime(int time) {
    return false;
  }

  @Override
  public int getTime() {
    return 0;
  }

  @Override
  public boolean isDefault() {
    return false;
  }

  @Override
  public Iterator<? extends T> iterator(Event e) {
    return new ArrayIterator<>(getAll(e));
  }

  @Override
  public boolean isLoopOf(String s) {
    return false;
  }

  @Override
  public Expression<?> getSource() {
    return source == null ? this : source;
  }

  @Override
  public Expression<? extends T> simplify() {
    return this;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return which.getPattern();
  }

  @Override
  public String toString() {
    return toString(null, false);
  }

  @Override
  public Class<?>[] acceptChange(Changer.ChangeMode mode) {
    return CustomExpressionSection.changerHandlers.containsKey(which)
        && CustomExpressionSection.changerHandlers.get(which).containsKey(mode)
        ? new Class[]{Object[].class} : null;
  }

  @Override
  public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
    Trigger changer = CustomExpressionSection.changerHandlers.getOrDefault(which, Collections.emptyMap()).get(mode);

    if (changer == null) {
      Skript.error(
          String.format("The custom expression '%s' no longer has a %s handler.",
              which.getPattern(), mode.name())
      );
    } else {
      changer.execute(new ExpressionChangeEvent(e, exprs, parseResult, delta));
    }
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    which = CustomExpressionSection.lookup(Util.getCurrentScript(), matchedPattern);

    if (which == null) {
      return false;
    }

    if (which.shouldAdaptArgument()) {
      Expression<?> lastExpression = exprs[exprs.length - 1];
      System.arraycopy(exprs, 0, exprs, 1, exprs.length - 1);
      exprs[0] = lastExpression;
    }

    this.exprs = Arrays.stream(exprs)
        .map(Util::defendExpression)
        .toArray(Expression[]::new);
    this.parseResult = parseResult;
    return Util.canInitSafely(this.exprs);
  }
}
