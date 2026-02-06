package org.skriptlang.reflect.syntax.custom.effect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import org.skriptlang.reflect.syntax.custom.shared.Continuable;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;

public class EffectTriggerEvent extends CustomSyntaxEvent implements Continuable {

	public final TriggerItem next;
	private boolean sync;

	public EffectTriggerEvent(
		Event event,
		Effect self,
		Expression<?>[] expressions,
		int matchedPattern,
		ParseResult parseResult,
		TriggerItem next
	) {
		super(event, self, expressions, matchedPattern, parseResult);
		this.next = next;
	}

	public TriggerItem next() {
		return next;
	}

	public boolean isSync() {
		return sync;
	}

	public void setSync(boolean sync) {
		this.sync = sync;
	}

	@Override
	public void setContinue(boolean b) {
		if (isSync())
			Skript.warning("Synchronous effects should not be continued. Call 'delay effect' to delay the effect's execution.");
		if (b)
			TriggerItem.walk(next, getDirectEvent());
	}

}
