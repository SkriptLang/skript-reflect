package org.skriptlang.reflect.syntax.custom.condition;

import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntax;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxCore;

@NoDoc
public class CustomCondition extends Condition implements CustomSyntax {

	private final CustomConditionInfo info;
	private final CustomSyntaxCore core;

	public CustomCondition(CustomConditionInfo info) {
		this.info = info;
		this.core = new CustomSyntaxCore(info);
	}

	@Override
	public CustomConditionInfo info() {
		return info;
	}

	@Override
	public boolean preInit() {
		return core.preInit() && super.preInit();
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!core.init(this, expressions, matchedPattern, parseResult))
			return false;
		if (info.property())
			setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event event) {
		Trigger trigger = info.checkTrigger();
		assert trigger != null;

		ConditionCheckEvent checkEvent = new ConditionCheckEvent(
			event,
			this,
			core.expressions(),
			core.matchedPattern(),
			core.parseResult()
		);
		core.walk(trigger, checkEvent);

		return checkEvent.isMarkedContinue() ^ checkEvent.isNegated() ^ isNegated();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return core.usedPattern();
	}

}
