package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import com.btk5h.skriptmirror.util.JavaUtil;
import org.bukkit.event.Event;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.Iterator;
import java.util.function.Predicate;

public class ExprExpression<T> implements Expression<T> {
	static {
		//noinspection unchecked
		Skript.registerExpression(ExprExpression.class, Object.class, ExpressionType.SIMPLE,
			"[the] expr[ession][(1¦s)](-| )<\\d+>");
	}

	private int index;
	private boolean plural;

	private final ExprExpression<?> source;
	private final Class<? extends T>[] types;
	private final Class<T> superType;

	@SuppressWarnings("unchecked")
	public ExprExpression() {
		this(null, ((Class<? extends T>) Object.class));
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	private ExprExpression(ExprExpression<?> source, Class<? extends T>... types) {
		if (source != null) {
			index = source.index;
			plural = source.plural;
		}

		this.source = source;
		this.types = types;
		this.superType = (Class<T>) Utils.getSuperType(types);
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		if (!getParser().isCurrentEvent(
			SyntaxParseEvent.class,
			ConditionCheckEvent.class,
			EffectTriggerEvent.class,
			EventTriggerEvent.class,
			ExpressionChangeEvent.class,
			ExpressionGetEvent.class
		)) {
			Skript.error("The expression 'expression' may only be used in a custom syntax.",
				ErrorQuality.SEMANTIC_ERROR);
			return false;
		}

		index = Utils.parseInt(parseResult.regexes.get(0).group(0));
		if (index <= 0) {
			Skript.error("The expression index must be a natural number.", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		index--;

		plural = parseResult.mark == 1;

		return true;
	}

	@Override
	public T getSingle(Event e) {
		T[] all = getAll(e);
		if (all.length == 0) {
			return null;
		}
		return all[0];
	}

	@Override
	public T[] getArray(Event e) {
		return getAll(e);
	}

	@Override
	public T[] getAll(Event e) {
		Expression<?> expr = getExpression(e);

		if (expr == null) {
			return JavaUtil.newArray(superType, 0);
		}

		return Converters.convert(expr.getAll(e), types, superType);
	}

	@Nullable
	Expression<?> getExpression(Event e) {
		Expression<?>[] expressions = ((CustomSyntaxEvent) e).getExpressions();
		if (index < expressions.length) {
			return expressions[index];
		}
		return null;
	}

	@Override
	public boolean isSingle() {
		return !plural;
	}

	@Override
	public boolean check(Event e, Predicate<? super T> c, boolean negated) {
		return SimpleExpression.check(getAll(e), c, negated, getAnd());
	}

	@Override
	public boolean check(Event e, Predicate<? super T> c) {
		return SimpleExpression.check(getAll(e), c, false, getAnd());
	}

	@Override
	public <R> Expression<? extends R> getConvertedExpression(Class<R>[] to) {
		return new ExprExpression<>(this, to);
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
		return new ArrayIterator<>(getAll(e));
	}

	@Override
	public boolean isLoopOf(String s) {
		return s.equalsIgnoreCase("expression");
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
	public String toString(Event e, boolean debug) {
		return "expression " + (index + 1);
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

}
