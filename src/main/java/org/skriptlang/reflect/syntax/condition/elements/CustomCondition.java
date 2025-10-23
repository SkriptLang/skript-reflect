package org.skriptlang.reflect.syntax.condition.elements;


import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.condition.ConditionSyntaxInfo;
import org.skriptlang.reflect.syntax.condition.elements.StructCustomCondition;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class CustomCondition extends Condition {

	private ConditionSyntaxInfo which;
	private Expression<?>[] exprs;
	private SkriptParser.ParseResult parseResult;
	private Object variablesMap;

	@Override
	public boolean check(Event e) {
		Trigger checker = StructCustomCondition.conditionHandlers.get(which);

		if (checker == null) {
			Skript.error(
				String.format("The custom condition '%s' no longer has a check handler.", which.getPattern())
			);
			return false;
		}

		if (which.isProperty()) {
			return checkByProperty(e, checker);
		}

		return checkByStandard(e, checker);
	}

	private boolean checkByStandard(Event e, Trigger checker) {
		ConditionCheckEvent conditionEvent = new ConditionCheckEvent(e, exprs, which.getMatchedPattern(), parseResult);
		SkriptReflection.putLocals(variablesMap, conditionEvent);
		checker.execute(conditionEvent);
		return conditionEvent.isMarkedContinue() ^ conditionEvent.isMarkedNegated() ^ which.isInverted();
	}

	private boolean checkByProperty(Event e, Trigger checker) {
		return exprs[0].check(e, o -> {
			Expression<?>[] localExprs = Arrays.copyOf(exprs, exprs.length);
			localExprs[0] = new SimpleLiteral<>(o, false);

			ConditionCheckEvent conditionEvent =
				new ConditionCheckEvent(e, localExprs, which.getMatchedPattern(), parseResult);
			SkriptReflection.putLocals(SkriptReflection.copyLocals(variablesMap), conditionEvent);
			checker.execute(conditionEvent);
			return conditionEvent.isMarkedContinue() ^ conditionEvent.isMarkedNegated();
		}, which.isInverted());
	}

	@Override
	public String toString(Event e, boolean debug) {
		return which.getPattern();
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		// prevent the user from using the placeholder pattern we register in order to satisfy the registration requirements
		if (matchedPattern == 0) {
			return false;
		}

		which = StructCustomCondition.lookup(SkriptUtil.getCurrentScript(), matchedPattern);

		if (which == null) {
			return false;
		}

		this.exprs = Arrays.stream(exprs)
			.map(SkriptUtil::defendExpression)
			.toArray(Expression[]::new);
		this.parseResult = parseResult;

		if (!SkriptUtil.canInitSafely(this.exprs)) {
			return false;
		}

		List<Supplier<Boolean>> suppliers = StructCustomCondition.usableSuppliers.get(which);
		if (suppliers != null && suppliers.size() != 0 && suppliers.stream().noneMatch(Supplier::get))
			return false;

		Boolean bool = StructCustomCondition.parseSectionLoaded.get(which);
		if (bool != null && !bool) {
			Skript.error("You can't use custom conditions with parse sections before they're loaded.");
			return false;
		}

		Trigger parseHandler = StructCustomCondition.parserHandlers.get(which);

		if (parseHandler != null) {
			SyntaxParseEvent event =
				new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, getParser().getCurrentEvents());

			TriggerItem.walk(parseHandler, event);
			variablesMap = SkriptReflection.removeLocals(event);

			return event.isMarkedContinue();
		}

		return true;
	}

}
