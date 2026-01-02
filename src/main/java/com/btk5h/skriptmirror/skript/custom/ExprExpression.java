package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.bukkit.event.Event;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ExprExpression extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprExpression.class, Object.class, ExpressionType.SIMPLE,
			"[the] expr[ession][plural:s](-| )<\\d+>");
	}

	private int index;
	private boolean isPlural;
	private Class<?> returnType;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentEvent(
			SyntaxParseEvent.class,
			ConditionCheckEvent.class,
			EffectTriggerEvent.class,
			EventTriggerEvent.class,
			ExpressionChangeEvent.class,
			ExpressionGetEvent.class
		)) {
			Skript.error("The expression 'expression' may only be used in a custom syntax structure");
			return false;
		}

		index = Utils.parseInt(parseResult.regexes.getFirst().group(0));
		if (index <= 0) {
			Skript.error("The expression index must be a natural number");
			return false;
		}
		index--;

		isPlural = parseResult.hasTag("plural");

		// TODO better return types
		returnType = Object.class;

		return true;
	}

	@Override
	protected Object @Nullable [] get(Event event) {
		Expression<?> expr = getExpression(event);
		if (expr == null) {
			return (Object[]) Array.newInstance(getReturnType(), 0);
		}
		Object[] values = expr.getAll(event);
		return Arrays.copyOf(values, values.length, (Class<? extends Object[]>) getReturnType().arrayType());
	}

	@Nullable
	Expression<?> getExpression(Event event) {
		Expression<?>[] expressions = ((CustomSyntaxEvent) event).getExpressions();
		if (index < expressions.length) {
			return expressions[index];
		}
		return null;
	}

	@Override
	public boolean isSingle() {
		return !isPlural;
	}

	@Override
	public Class<?> getReturnType() {
		return returnType;
	}

	@Override
	public String toString(Event event, boolean debug) {
		return "expression " + (index + 1);
	}

}
