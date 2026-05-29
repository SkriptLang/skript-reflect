package org.skriptlang.reflect.syntax.custom.effect;

import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntax;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxCore;

@NoDoc
public class CustomEffect extends Effect implements CustomSyntax {

	private final CustomEffectInfo info;
	private final CustomSyntaxCore core;

	public CustomEffect(CustomEffectInfo info) {
		this.info = info;
		this.core = new CustomSyntaxCore(info);
	}

	@Override
	public CustomEffectInfo info() {
		return info;
	}

	@Override
	public boolean preInit() {
		return core.preInit() && super.preInit();
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		return core.init(this, expressions, matchedPattern, parseResult);
	}

	@Override
	protected void execute(Event event) {
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		EffectTriggerEvent effectEvent = invokeEffect(event);

		if (effectEvent.isSync())
			return getNext();

		Object locals = Variables.copyLocalVariables(effectEvent.getDirectEvent());
		new Thread(() -> {
			try {
				Thread.sleep(1);
				if (!effectEvent.isMarkedContinue())
					Variables.setLocalVariables(effectEvent.getDirectEvent(), locals);
			} catch (InterruptedException ignored) {}
		}).start();
		return null;
	}

	private EffectTriggerEvent invokeEffect(Event event) {
		assert info.executeTrigger() != null;

		EffectTriggerEvent triggerEvent = new EffectTriggerEvent(
			event,
			this,
			core.expressions(),
			core.matchedPattern(),
			core.parseResult(),
			getNext()
		);

		if (core.parseEvent() == null) {
			TriggerItem.walk(info.executeTrigger(), triggerEvent);
			return triggerEvent;
		}
		Variables.withLocalVariables(core.parseEvent(), triggerEvent,
			() -> TriggerItem.walk(info.executeTrigger(), triggerEvent));
		return triggerEvent;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return core.usedPattern();
	}

}
