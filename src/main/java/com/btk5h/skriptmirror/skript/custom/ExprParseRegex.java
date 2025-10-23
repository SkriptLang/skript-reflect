package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.bukkit.event.Event;

import java.util.List;
import java.util.regex.MatchResult;

public class ExprParseRegex extends SimpleExpression<String> {
	static {
		Skript.registerExpression(ExprParseRegex.class, String.class, ExpressionType.SIMPLE,
			"[the] [parse[r]] (regex|regular expression)(-| )<\\d+>");
	}

	private int index;

	@Override
	protected String[] get(Event e) {
		List<MatchResult> regexes = ((CustomSyntaxEvent) e).getParseResult().regexes;
		if (index < regexes.size()) {
			MatchResult match = regexes.get(index);
			int groupCount = match.groupCount();
			String[] groups = new String[groupCount];

			for (int i = 1; i <= groupCount; i++) {
				groups[i - 1] = match.group(i);
			}

			return groups;
		}
		return new String[0];
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
	public String toString(Event e, boolean debug) {
		return "parser mark";
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
			Skript.error("The parsed regular expression may only be used in custom syntax.",
				ErrorQuality.SEMANTIC_ERROR);
			return false;
		}

		index = Utils.parseInt(parseResult.regexes.get(0).group(0));
		if (index <= 0) {
			Skript.error("The expression index must be a natural number.", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		index--;
		return true;
	}
}
