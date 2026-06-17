package org.skriptlang.reflect.syntax.custom.event.elements;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EffCallEvent extends Effect {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffCallEvent.class)
			.supplier(EffCallEvent::new)
			.addPattern("call [event] %events%")
			.build());
	}

	private Expression<Event> events;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		//noinspection unchecked
		events = (Expression<Event>) expressions[0];
		return true;
	}

	@Override
	protected void execute(Event event) {
		for (Event e : events.getArray(event))
			Bukkit.getPluginManager().callEvent(e);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "call event " + events.toString(event, debug);
	}

}
