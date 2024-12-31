package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.conditions.base.PropertyCondition;
import org.bukkit.event.Cancellable;

// So I should've used PropertyCondition<Event>, but if you want to implement the check method, you'd be overriding
// the 'normal' Condition#check(Event) method, which is rightfully declared final in PropertyCondition.
// They probably should've used some other name then check, but hey, I can't help it without breaking
// backwards compatibility. And for some reason this works, so I'll keep it.
public class CondEventCancelled<T> extends PropertyCondition<T> {

	static {
		register(CondEventCancelled.class, "cancelled", "events");
	}

	@Override
	public boolean check(T event) {
		return event instanceof Cancellable && ((Cancellable) event).isCancelled();
	}

	@Override
	protected String getPropertyName() {
		return "cancelled";
	}

}
