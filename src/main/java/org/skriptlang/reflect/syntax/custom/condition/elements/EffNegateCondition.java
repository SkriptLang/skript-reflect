package org.skriptlang.reflect.syntax.custom.condition.elements;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.condition.ConditionCheckEvent;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxOrigin;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EffNegateCondition extends Effect implements EventRestrictedSyntax {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffNegateCondition.class)
			.origin(SyntaxOrigin.of(SkriptMirror.getAddonInstance()))
			.supplier(EffNegateCondition::new)
			.addPattern("negate [the] [current] condition")
			.build());
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return CollectionUtils.array(ConditionCheckEvent.class);
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	protected void execute(Event event) {
		if (!(event instanceof ConditionCheckEvent checkEvent))
			return;
		checkEvent.markNegated();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "negate the current condition";
	}

}
