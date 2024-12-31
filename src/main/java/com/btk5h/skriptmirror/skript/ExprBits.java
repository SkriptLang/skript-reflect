package com.btk5h.skriptmirror.skript;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

import java.util.Arrays;

public class ExprBits extends SimpleExpression<Number> {
	static {
		// parse mark also indicates the offset of the first argument
		PropertyExpression.register(ExprBits.class, Number.class,
			"(0¦bit %-number%|1¦bit(s| range) [from] %-number%( to |[ ]-[ ])%-number%)", "numbers");
	}

	private Expression<Number> numbers;
	private Expression<Number> from;
	private Expression<Number> to;

	@Override
	protected Number[] get(Event e) {
		Number f = from.getSingle(e);
		Number t = to.getSingle(e);

		if (f == null || t == null) {
			return null;
		}

		long mask = getRangeMaskIndexed(f.intValue(), t.intValue());

		return Arrays.stream(numbers.getArray(e))
			.map(l -> (l.longValue() & mask) >>> f.intValue())
			.toArray(Number[]::new);
	}

	private static long getRangeMaskOrdinal(int from) {
		if (from <= 0 || from >= Long.SIZE) {
			return 0;
		}

		return (1L << from) - 1;
	}

	private static long getRangeMaskOrdinal(int from, int to) {
		if (from > to) {
			return 0;
		}

		return getRangeMaskOrdinal(to) - getRangeMaskOrdinal(from - 1);
	}

	private static long getRangeMaskIndexed(int from, int to) {
		return getRangeMaskOrdinal(from + 1, to + 1);
	}

	@Override
	public boolean isSingle() {
		return numbers.isSingle();
	}

	@Override
	public String toString(Event e, boolean debug) {
		return String.format("the bits %s to %s of %s",
			from.toString(e, debug), to.toString(e, debug), numbers.toString(e, debug));
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	public Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (isSingle() && (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE)
				&& Changer.ChangerUtils.acceptsChange(numbers, Changer.ChangeMode.SET, Number.class)) {
			return new Class[]{Number.class, Boolean.class};
		}
		return null;
	}

	@Override
	public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
		Number num = numbers.getSingle(e);
		Number f = from.getSingle(e);
		Number t = to.getSingle(e);

		if (num == null || f == null || t == null) {
			return;
		}

		long mask = getRangeMaskIndexed(f.intValue(), t.intValue());

		long number = num.longValue();

		switch (mode) {
			case SET:
				if (delta[0] instanceof Number) {
					number &= ~mask;
					mask &= (((Number) delta[0]).longValue()) << f.intValue();
				} else if (delta[0] instanceof Boolean) {
					if (!((Boolean) delta[0])) {
						mask = ~mask;
					}
				} else {
					throw new IllegalStateException();
				}

				number |= mask;
				break;
			case DELETE:
				number &= ~mask;
				break;
			default:
				throw new IllegalStateException();
		}

		numbers.change(e, new Object[]{number}, Changer.ChangeMode.SET);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		numbers = (Expression<Number>) exprs[matchedPattern == 1 ? 0 : 3];


		// it just so happens that we can use matchedPattern to determine the offset of the arguments
		from = (Expression<Number>) exprs[parseResult.mark + matchedPattern];
		to = parseResult.mark == 0 ? from : (Expression<Number>) exprs[2 + matchedPattern];

		return true;
	}
}
