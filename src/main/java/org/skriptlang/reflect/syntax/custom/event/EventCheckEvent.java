package org.skriptlang.reflect.syntax.custom.event;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.custom.shared.Continuable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;

public class EventCheckEvent extends CustomSyntaxEvent implements Continuable {

	private boolean markedContinue;

	public EventCheckEvent(BukkitCustomEvent event, Expression<?>[] expressions, int matchedPattern, ParseResult parseResult) {
		super(event, expressions, matchedPattern, parseResult);
	}

	@Override
	public BukkitCustomEvent getDirectEvent() {
		return (BukkitCustomEvent) super.getDirectEvent();
	}

	public boolean isMarkedContinue() {
		return markedContinue;
	}

	@Override
	public void setContinue(boolean b) {
		markedContinue = b;
	}

}
