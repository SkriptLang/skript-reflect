package org.skriptlang.reflect.syntax.custom.shared.elements.expressions;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprRawExpression extends SimpleExpression<Expression> {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, PropertyExpression.infoBuilder(
				ExprRawExpression.class,
				Expression.class,
				"(raw|underlying) expression[s]",
				"objects",
				false
			)
			.addPattern("raw expression[s] %objects%")
			.supplier(ExprRawExpression::new)
			.build());
	}

	private Expression<?> expr;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		expr = LiteralUtils.defendExpression(expressions[0]);
		return LiteralUtils.canInitSafely(expressions);
	}

	@Override
	protected Expression @Nullable [] get(Event event) {
		if (event instanceof CustomSyntaxEvent syntaxEvent && expr instanceof ExprExpression exprExpr) {
			Expression<?> unwrapped = syntaxEvent.expression(exprExpr.index());
			return unwrapped != null ? new Expression[] {unwrapped.getSource()} : new Expression[0];
		}
		return new Expression[] {expr};
	}

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
		return expr instanceof ExprExpression ? new Class[] {Object[].class} : null;
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
		if (!(event instanceof CustomSyntaxEvent syntaxEvent && expr instanceof ExprExpression exprExpr))
			return;

		Expression<?> underlyingExpr = syntaxEvent.expression(exprExpr.index());
		if (underlyingExpr == null)
			return;
		Expression<?> source = underlyingExpr.getSource();
		Event unwrappedEvent = syntaxEvent.getDirectEvent();
		try {
			source.acceptChange(mode);
			source.change(unwrappedEvent, delta, mode);
		} catch (Throwable throwable) {
			ExprJavaCall.lastError = throwable;
		}
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Expression> getReturnType() {
		return Expression.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "the underlying expression of " + expr.toString(event, debug);
	}

}
