package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.WrappedEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public abstract class CustomSyntaxEvent extends WrappedEvent {

	private final SyntaxElement self;
	private final Expression<?>[] expressions;
	private final int matchedPattern;
	private final ParseResult parseResult;

	public CustomSyntaxEvent(
		Event event,
		SyntaxElement self,
		Expression<?>[] expressions,
		int matchedPattern,
		ParseResult parseResult
	) {
		super(event);
		this.self = self;
		this.expressions = Arrays.stream(expressions)
			.map(LazyExpression::new)
			.toArray(Expression<?>[]::new);
		this.matchedPattern = matchedPattern;
		this.parseResult = parseResult;
	}

	public SyntaxElement self() {
		return self;
	}

	public Expression<?>[] expressions() {
		return expressions;
	}

	public @Nullable Expression<?> expression(int index) {
		if (index < 0 || index >= expressions.length)
			return null;
		return expressions[index];
	}

	public int matchedPattern() {
		return matchedPattern;
	}

	public ParseResult parseResult() {
		return parseResult;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		throw new UnsupportedOperationException();
	}

	public static class LazyExpression extends WrapperExpression<Object> {

		private transient Object[] array, all;

		public LazyExpression(Expression<?> source) {
			setExpr(source != null ? source : new EmptyExpression());
		}

		@Override
		protected Object[] get(Event event) {
			if (array == null)
				array = getExpr().getArray(event);
			return array;
		}

		@Override
		public Object[] getAll(Event event) {
			if (all == null)
				all = getExpr().getAll(event);
			return all;
		}

		@Override
		public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString(@Nullable Event event, boolean debug) {
			return getExpr().toString(event, debug);
		}

	}

	public static class EmptyExpression extends SimpleExpression<Object> {

		@Override
		protected Object[] get(Event event) {
			return new Object[0];
		}

		@Override
		public boolean isSingle() {
			return false;
		}

		@Override
		public Class<?> getReturnType() {
			return Object.class;
		}

		@Override
		public String toString(@Nullable Event event, boolean debug) {
			return "";
		}

		@Override
		public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			return false;
		}

	}

}
