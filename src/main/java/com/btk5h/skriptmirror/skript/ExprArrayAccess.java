package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.util.JavaUtil;
import org.bukkit.event.Event;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.function.Predicate;

public class ExprArrayAccess<T> implements Expression<T> {

	static {
		//noinspection unchecked
		Skript.registerExpression(ExprArrayAccess.class, Object.class, ExpressionType.COMBINED,
			"%javaobject%\\[%number%\\]");
	}

	private Expression<ObjectWrapper> arrays;
	private Expression<Number> index;

	private final ExprArrayAccess<?> source;
	private final Class<? extends T>[] types;
	private final Class<T> superType;

	@SuppressWarnings("unchecked")
	public ExprArrayAccess() {
		this(null, (Class<? extends T>) Object.class);
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	private ExprArrayAccess(ExprArrayAccess<?> source, Class<? extends T>... types) {
		this.source = source;

		if (source != null) {
			this.arrays = source.arrays;
			this.index = source.index;
		}

		this.types = types;
		this.superType = (Class<T>) Utils.getSuperType(types);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		arrays = (Expression<ObjectWrapper>) exprs[0];
		index = (Expression<Number>) exprs[1];
		return true;
	}

	@Override
	public T getSingle(Event e) {
		ObjectWrapper wrapper = arrays.getSingle(e);
		Number number = index.getSingle(e);

		if (wrapper == null || number == null || !wrapper.isArray()) {
			return null;
		}

		Object array = wrapper.get();
		int length = Array.getLength(array);

		int i = number.intValue();
		if (i < 0 || i >= length) {
			return null;
		}

		return Converters.convert(Array.get(array, i), types);
	}

	@Override
	public T[] getArray(Event e) {
		T single = getSingle(e);

		if (single == null) {
			return JavaUtil.newArray(superType, 0);
		}

		T[] all = JavaUtil.newArray(superType, 1);
		all[0] = single;

		return all;
	}

	@Override
	public T[] getAll(Event e) {
		return getArray(e);
	}

	@Override
	public boolean isSingle() {
		return true;
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
		return new ExprArrayAccess<>(this, to);
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
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
			return new Class[]{Object.class};
		}
		return null;
	}

	@Override
	public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
		ObjectWrapper wrapper = arrays.getSingle(e);
		Number number = index.getSingle(e);

		if (wrapper == null || number == null || !wrapper.isArray()) {
			return;
		}

		Object array = wrapper.get();
		int length = Array.getLength(array);

		int i = number.intValue();
		if (i < 0 || i >= length) {
			return;
		}

		switch (mode) {
			case SET:
				Class<?> to = array.getClass().getComponentType();
				if (JavaUtil.canConvert(delta[0], to)) {
					Object converted = JavaUtil.convert(delta[0], to);
					Array.set(array, i, converted);
				}
				break;
			case DELETE:
				Array.set(array, i, null);
				break;
		}
	}

	@Override
	public String toString(Event e, boolean debug) {
		return arrays.toString(e, debug) + "[" + index.toString(e, debug) + "]";
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

}
