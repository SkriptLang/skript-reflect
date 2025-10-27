package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.bukkit.event.Event;

public class ExprParseTags extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprParseTags.class, String.class, ExpressionType.SIMPLE, "[the] parse[r] tags");
	}

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
			Skript.error("The parse tags may only be used in custom syntax");
			return false;
		}
		return true;
	}

	@Override
	@Nullable
	protected String[] get(Event e) {
		return ((CustomSyntaxEvent) e).getParseResult().tags.toArray(new String[0]);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "parse tags";
	}

}
