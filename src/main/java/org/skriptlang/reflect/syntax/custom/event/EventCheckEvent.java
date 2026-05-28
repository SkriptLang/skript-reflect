package org.skriptlang.reflect.syntax.custom.event;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.skriptlang.reflect.syntax.custom.shared.Continuable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;

public class EventCheckEvent extends CustomSyntaxEvent implements Continuable {

	private boolean markedContinue;

	public EventCheckEvent(
		BukkitCustomEvent event,
		SkriptEvent self,
		Expression<?>[] expressions,
		int matchedPattern,
		ParseResult parseResult
	) {
		super(event, self, expressions, matchedPattern, parseResult);
	}

	@Override
	public BukkitCustomEvent getDirectEvent() {
		return (BukkitCustomEvent) super.getDirectEvent();
	}

	@Override
	public boolean isMarkedContinue() {
		return markedContinue;
	}

	@Override
	public void setContinue(boolean b) {
		markedContinue = b;
	}

}
