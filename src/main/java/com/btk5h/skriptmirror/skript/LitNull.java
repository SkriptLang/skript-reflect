package com.btk5h.skriptmirror.skript;

import com.btk5h.skriptmirror.Util;

import org.bukkit.event.Event;

import java.util.Iterator;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;

public class LitNull<T> implements Literal<T> {
  static {
    //noinspection unchecked
    Skript.registerExpression(LitNull.class, Object.class, ExpressionType.SIMPLE, "(null|nothing)");
  }

  private final LitNull<?> source;
  private final Class<? extends T> superType;

  @SuppressWarnings("unchecked")
  public LitNull() {
    this(null, (Class<? extends T>) Object.class);
  }

  @SuppressWarnings("unchecked")
  @SafeVarargs
  private LitNull(LitNull<?> source, Class<? extends T>... types) {
    this.source = source;
    this.superType = (Class<? extends T>) Utils.getSuperType(types);
  }

  @Override
  public T[] getArray() {
    return getAll();
  }

  @Override
  public T getSingle() {
    return null;
  }

  @Override
  public T getSingle(Event e) {
    return getSingle();
  }

  @Override
  public T[] getArray(Event e) {
    return getArray();
  }

  @Override
  public T[] getAll(Event e) {
    return getAll();
  }

  @Override
  public boolean isSingle() {
    return true;
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
  public <R> Literal<? extends R> getConvertedExpression(Class<R>[] to) {
    return new LitNull<>(this, to);
  }

  @Override
  public Class<? extends T> getReturnType() {
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
    return new ArrayIterator<T>(getAll());
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
  public Class<?>[] acceptChange(Changer.ChangeMode mode) {
    return null;
  }

  @Override
  public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T[] getAll() {
    return Util.newArray(superType, 0);
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "null";
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    return true;
  }
}
