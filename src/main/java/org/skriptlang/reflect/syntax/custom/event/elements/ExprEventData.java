package org.skriptlang.reflect.syntax.custom.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.custom.event.EventCheckEvent;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprEventData extends SimpleExpression<Object> {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprEventData.class, Object.class)
			.supplier(ExprEventData::new)
			.addPattern("[extra] [event(-| )]data %string%")
			.build());
	}

	private Expression<String> key;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentEvent(BukkitCustomEvent.class, EventCheckEvent.class)) {
			Skript.error("The event data expression can only be used in a custom event");
			return false;
		}
		//noinspection unchecked
		key = (Expression<String>) expressions[0];
		return true;
	}

	@Override
	protected Object @Nullable [] get(Event event) {
		String key = this.key.getSingle(event);
		if (key == null)
			return new Object[0];

		if (event instanceof BukkitCustomEvent customEvent) {
			return new Object[] {customEvent.getData(key)};
		} else if (event instanceof EventCheckEvent checkEvent) {
			return new Object[] {checkEvent.getDirectEvent().getData(key)};
		}

		return new Object[0];
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<?> getReturnType() {
		return Object.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "event data " + key.toString(event, debug);
	}

}
