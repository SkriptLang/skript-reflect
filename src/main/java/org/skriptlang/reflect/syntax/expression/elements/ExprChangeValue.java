package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ExprChangeValue extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprChangeValue.class, Object.class, ExpressionType.SIMPLE,
			"[the] change value[plural:s]");
	}

	private boolean isPlural;
	private Class<?> returnType;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		ParserInstance parser = getParser();
		if (!parser.isCurrentEvent(ExpressionChangeEvent.class)) {
			Skript.error("The change value may only be used in a change handler");
			return false;
		}

		isPlural = parseResult.hasTag("plural");
		returnType = Object.class;
		if (parser.getCurrentStructure() instanceof StructCustomExpression structCustomExpression) {
			if (structCustomExpression.returnType != null) {
				returnType = structCustomExpression.returnType;
				if (returnType.isArray()) {
					returnType = returnType.getComponentType();
				}
			}
		}

		return true;
	}

	@Override
	protected Object[] get(Event event) {
		Object[] delta = ((ExpressionChangeEvent) event).getDelta();
		if (delta == null) {
			return (Object[]) Array.newInstance(getReturnType(), 0);
		}
		return Arrays.copyOf(delta, delta.length, (Class<? extends Object[]>) getReturnType().arrayType());
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
		return "the change value" + (isPlural ? "s" : "");
	}

}
