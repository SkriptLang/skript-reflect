package org.skriptlang.reflect.syntax.custom.event;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.variables.Variables;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntax;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxCore;

public class CustomEvent extends SkriptEvent implements CustomSyntax {

	private final CustomEventInfo info;
	private final CustomSyntaxCore core;

	public CustomEvent(CustomEventInfo info) {
		this.info = info;
		this.core = new CustomSyntaxCore(info);
	}

	@Override
	public CustomEventInfo info() {
		return info;
	}

	@Override
	public boolean preInit() {
		return core.preInit() && super.preInit();
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
		return core.init(this, args, matchedPattern, parseResult);
	}

	@Override
	public boolean load() {
		if (!shouldLoadEvent())
			return false;

		try {
			CustomEventManager.setCurrentEvent(info.identifier());
			return super.load();
		} finally {
			CustomEventManager.deleteCurrentEvent();
		}
	}

	@Override
	public boolean check(Event event) {
		if (!(event instanceof BukkitCustomEvent customEvent) || !customEvent.identifier().equals(info.identifier()))
			return false;

		Trigger checkTrigger = info.checkTrigger();
		if (checkTrigger == null)
			return true;

		EventCheckEvent checkEvent = new EventCheckEvent(
			customEvent,
			this,
			core.expressions(),
			core.matchedPattern(),
			core.parseResult()
		);

		if (core.parseEvent() == null) {
			Trigger.walk(checkTrigger, checkEvent);
		} else {
			Variables.withLocalVariables(core.parseEvent(), checkEvent, () -> Trigger.walk(checkTrigger, checkEvent));
		}
		return checkEvent.isMarkedContinue();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return core.usedPattern();
	}

}
