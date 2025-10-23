package org.skriptlang.reflect.syntax.effect.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.effect.EffectSyntaxInfo;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.effect.elements.StructCustomEffect;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class CustomEffect extends Effect {

	private EffectSyntaxInfo which;
	private Expression<?>[] exprs;
	private SkriptParser.ParseResult parseResult;
	private Object variablesMap;

	@Override
	protected void execute(Event e) {
		// for effect commands
		invokeEffect(e);
	}

	@Override
	protected TriggerItem walk(Event e) {
		EffectTriggerEvent effectEvent = invokeEffect(e);

		if (effectEvent.isSync()) {
			return getNext();
		}

		Object localVars = SkriptReflection.getLocals(effectEvent.getDirectEvent());
		new Thread(() -> {
			try {
				Thread.sleep(1);
				if (!effectEvent.hasContinued())
					SkriptReflection.putLocals(localVars, effectEvent.getDirectEvent());
			} catch (InterruptedException ignored) { }
		}).start();
		return null;
	}

	private EffectTriggerEvent invokeEffect(Event e) {
		Trigger trigger = StructCustomEffect.effectHandlers.get(which);
		EffectTriggerEvent effectEvent =
			new EffectTriggerEvent(e, exprs, which.getMatchedPattern(), parseResult, which.getPattern(), getNext());
		if (trigger == null) {
			Skript.error(String.format("The custom effect '%s' no longer has a handler.", which));
		} else {
			SkriptReflection.putLocals(SkriptReflection.copyLocals(variablesMap), effectEvent);
			trigger.execute(effectEvent);
		}
		return effectEvent;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return which.getPattern();
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		// prevent the user from using the placeholder pattern we register in order to satisfy the registration requirements
		if (matchedPattern == 0) {
			return false;
		}

		which = StructCustomEffect.lookup(SkriptUtil.getCurrentScript(), matchedPattern);

		if (which == null) {
			return false;
		}

		this.exprs = Arrays.stream(exprs)
			.map(SkriptUtil::defendExpression)
			.toArray(Expression[]::new);
		this.parseResult = parseResult;

		if (!SkriptUtil.canInitSafely(this.exprs)) {
			return false;
		}

		List<Supplier<Boolean>> suppliers = StructCustomEffect.usableSuppliers.get(which);
		if (suppliers != null && suppliers.size() != 0 && suppliers.stream().noneMatch(Supplier::get))
			return false;

		Boolean bool = StructCustomEffect.parseSectionLoaded.get(which);
		if (bool != null && !bool) {
			Skript.error("You can't use custom effects with parse sections before they're loaded.");
			return false;
		}

		Trigger parseHandler = StructCustomEffect.parserHandlers.get(which);

		if (parseHandler != null) {
			SyntaxParseEvent event =
				new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, getParser().getCurrentEvents());

			TriggerItem.walk(parseHandler, event);
			variablesMap = SkriptReflection.removeLocals(event);

			return event.isMarkedContinue();
		}

		return true;
	}

}
