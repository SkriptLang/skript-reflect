package org.skriptlang.reflect.syntax.custom.effect.elements;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.effect.EffectTriggerEvent;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EffDelayEffect extends Effect implements EventRestrictedSyntax {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffDelayEffect.class)
			.supplier(EffDelayEffect::new)
			.addPattern("delay [the] [current] effect")
			.build());
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return CollectionUtils.array(EffectTriggerEvent.class);
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		return true;
	}

	@Override
	protected void execute(Event event) {
		if (!(event instanceof EffectTriggerEvent triggerEvent))
			return;
		triggerEvent.setSync(true);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "delay the current effect";
	}

}
