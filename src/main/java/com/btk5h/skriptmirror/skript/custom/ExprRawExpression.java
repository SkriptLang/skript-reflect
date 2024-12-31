package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.WrappedEvent;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

public class ExprRawExpression extends SimpleExpression<Expression> {
	static {
		Skript.registerExpression(ExprRawExpression.class, Expression.class, ExpressionType.PATTERN_MATCHES_EVERYTHING,
				"[the] (raw|underlying) expression[s] of %objects%",
				"%objects%'[s] (raw|underlying) expression[s]",
				"[the] raw [expression] %objects%");
	}

	private Expression<?> expr;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (matchedPattern == 2)
			Skript.warning("Using 'raw %objects%' is deprecated, please use 'the (raw|underlying) expression of %objects%' instead. " +
					"If you meant to use Skript's 'raw %strings%' expression, try 'raw string within %objects%'.");
		expr = SkriptUtil.defendExpression(exprs[0]);
		return SkriptUtil.canInitSafely(expr);
	}

	@Override
	protected Expression<?>[] get(Event event) {
		Expression<?> expr = this.expr;
		if (expr instanceof ExprExpression<?> exprExpr && event instanceof CustomSyntaxEvent) {
			expr = exprExpr.getExpression(event);
			if (expr == null)
				return null;
			expr = expr.getSource();
		}
		return new Expression[] {expr};
	}

	@Override
	public Class<?>[] acceptChange(ChangeMode changeMode) {
		return expr instanceof ExprExpression ? new Class[] {Object[].class} : null;
	}

	@Override
	public void change(Event event, Object[] delta, ChangeMode changeMode) {
		if (!(expr instanceof ExprExpression && event instanceof CustomSyntaxEvent))
			return;

		Expression<?> expr = ((ExprExpression<?>) this.expr).getExpression(event);
		if (expr == null)
			return;
		Expression<?> source = expr.getSource();

		Event unwrappedEvent = ((WrappedEvent) event).getDirectEvent();
		// Ensure acceptChange has been called before change
		try {
			source.acceptChange(changeMode);
			source.change(unwrappedEvent, delta, changeMode);
		} catch (Throwable throwable) {
			ExprJavaCall.lastError = throwable;
		}
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Expression> getReturnType() {
		return Expression.class;
	}

	@Override
	public String toString(Event event, boolean debug) {
		return "the underlying expression of " + expr.toString(event, debug);
	}

}
