package org.skriptlang.reflect.syntax.custom.event.elements;

import ch.njol.skript.conditions.base.PropertyCondition;
import org.bukkit.event.Cancellable;
import org.skriptlang.skript.registration.SyntaxRegistry;

// This should be extending PropertyCondition<Event> instead of PropertyCondition<Object>, but when trying to implement
// the check method for PropertyCondition<Event>, it would override the base Condition#check(Event), which is
// rightfully declared final in PropertyCondition.
public class CondEventCancelled extends PropertyCondition<Object> {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.CONDITION, infoBuilder(CondEventCancelled.class, PropertyType.BE, "cancel[l]ed", "events")
			.supplier(CondEventCancelled::new)
			.build());
	}

	@Override
	public boolean check(Object event) {
		return event instanceof Cancellable && ((Cancellable) event).isCancelled();
	}

	@Override
	protected String getPropertyName() {
		return "cancelled";
	}

}
