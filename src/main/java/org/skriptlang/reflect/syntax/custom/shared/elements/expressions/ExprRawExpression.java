package org.skriptlang.reflect.syntax.custom.shared.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprRawExpression extends SimpleExpression<Expression> {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprRawExpression.class, Expression.class)
			.origin(Origin.of(SkriptMirror.getAddonInstance()))
			.supplier(ExprRawExpression::new)
			.addPattern("[the] (raw|underlying) expression[s] of %objects%")
			.addPattern("%objects%'[s] (raw|underlying) expression[s]")
			.addPattern("[the] raw [expression] %objects%")
			.priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)
			.build());
	}

	private Expression<?> expr;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (matchedPattern == 2) {
			Skript.warning(
				"Using 'raw %objects%' is deprecated, " +
				"please use 'the (raw|underlying) expression of %objects%' instead. " +
				"If you meant to use Skript's 'raw %strings%' expression, try 'raw string within %objects%'."
			);
		}
		return true;
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
	public Class<? extends Expression> getReturnType() {
		return Expression.class;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "the underlying expression of " + expr.toString(event, debug);
	}

}
