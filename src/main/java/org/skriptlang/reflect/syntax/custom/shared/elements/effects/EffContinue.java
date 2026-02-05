package org.skriptlang.reflect.syntax.custom.shared.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.Continuable;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

public class EffContinue extends Effect {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffContinue.class)
			.origin(Origin.of(SkriptMirror.getAddonInstance()))
			.supplier(EffContinue::new)
			.addPattern("continue")
			.priority(Priority.before(SyntaxInfo.COMBINED))
			.build());
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!CollectionUtils.containsAnySuperclass(new Class[] {Continuable.class}, getParser().getCurrentEvents())) {
			Skript.error("Continue may only be used in loops, custom effects, custom syntax parse sections and custom conditions.");
			return false;
		}
		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		if (event instanceof Continuable continuable) {
			continuable.markContinue();
		}
		return null;
	}

	@Override
	protected void execute(Event event) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "continue";
	}

}
