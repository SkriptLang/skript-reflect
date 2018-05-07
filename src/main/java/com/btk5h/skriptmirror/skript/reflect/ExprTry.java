package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;

import java.util.Iterator;

public class ExprTry<T> implements Expression<T> {
  static {
    //noinspection unchecked
    Skript.registerExpression(ExprTry.class, Object.class, ExpressionType.COMBINED, "try %object%");
  }

  private Expression<? extends T> expr;

  private final ExprTry<?> source;

  @SuppressWarnings("unchecked")
  public ExprTry() {
    this(null, (Class<T>) Object.class);
  }

  @SuppressWarnings("unchecked")
  @SafeVarargs
  private ExprTry(ExprTry<?> source, Class<T>... types) {
    this.source = source;

    if (source != null) {
      this.expr = source.expr.getConvertedExpression(types);
    }
  }

  @Override
  public T getSingle(Event e) {
    return expr.getSingle(e);
  }

  @Override
  public T[] getArray(Event e) {
    return expr.getArray(e);
  }

  @Override
  public T[] getAll(Event e) {
    return expr.getAll(e);
  }

  @Override
  public boolean isSingle() {
    return expr.isSingle();
  }

  @Override
  public boolean check(Event e, Checker<? super T> c, boolean negated) {
    return expr.check(e, c, negated);
  }

  @Override
  public boolean check(Event e, Checker<? super T> c) {
    return expr.check(e, c);
  }

  @Override
  public <R> Expression<? extends R> getConvertedExpression(Class<R>[] to) {
    return new ExprTry<>(this, to);
  }

  @Override
  public Class<? extends T> getReturnType() {
    return expr.getReturnType();
  }

  @Override
  public boolean getAnd() {
    return expr.getAnd();
  }

  @Override
  public boolean setTime(int time) {
    return expr.setTime(time);
  }

  @Override
  public int getTime() {
    return expr.getTime();
  }

  @Override
  public boolean isDefault() {
    return expr.isDefault();
  }

  @Override
  public Iterator<? extends T> iterator(Event e) {
    return expr.iterator(e);
  }

  @Override
  public boolean isLoopOf(String s) {
    return expr.isLoopOf(s);
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
  public Class<?>[] acceptChange(Changer.ChangeMode mode) {
    return expr.acceptChange(mode);
  }

  @Override
  public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
    expr.change(e, delta, mode);
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "try " + expr.toString(e, debug);
  }

  @Override
  public String toString() {
    return toString(null, false);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    expr = Util.defendExpression(exprs[0]);

    if (!Util.canInitSafely(expr)) {
      return false;
    }

    if (!(expr instanceof ExprJavaCall)) {
      Skript.error("Try may only be used with Java calls");
      return false;
    }

    ((ExprJavaCall) expr).setSuppressErrors(true);
    return true;
  }
}
