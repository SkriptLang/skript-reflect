package org.skriptlang.reflect.syntax.custom.effect;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntax;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxCore;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxOrigin;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.function.Predicate;

public class CustomEffect extends Effect implements CustomSyntax<SyntaxInfo<? extends Effect>> {

	private final CustomSyntaxCore core;
	private final SyntaxInfo<CustomEffect> info;
	private Trigger executeTrigger;

	public CustomEffect(
		String[] patterns,
		boolean hasParseSection,
		@Nullable Script script,
		Predicate<ParserInstance> usableInPredicate
	) {
		this(
			new CustomSyntaxCore(
				patterns,
				hasParseSection,
				script,
				usableInPredicate
			),
			null
		);
	}

	public CustomEffect(CustomSyntaxCore core, @Nullable Trigger executeTrigger) {
		this.core = core;
		this.executeTrigger = executeTrigger;
		this.info = SyntaxInfo.builder(CustomEffect.class)
			.origin(SyntaxOrigin.of(SkriptMirror.getAddonInstance()))
			.supplier(this::copy)
			.addPatterns(core.patterns())
			.priority(core.priority())
			.build();
	}

	@Override
	public SyntaxRegistry.Key<SyntaxInfo<? extends Effect>> key() {
		return SyntaxRegistry.EFFECT;
	}

	@Override
	public SyntaxInfo<? extends Effect> info() {
		return info;
	}

	@Override
	public CustomEffect copy() {
		return new CustomEffect(core.copy(), executeTrigger);
	}

	@Override
	public Trigger parseTrigger() {
		return core.parseTrigger();
	}

	@Override
	public void parseTrigger(Trigger parseTrigger) {
		core.parseTrigger(parseTrigger);
	}

	public Trigger executeTrigger() {
		return executeTrigger;
	}

	public void executeTrigger(Trigger executeTrigger) {
		if (this.executeTrigger != null)
			throw new IllegalStateException("Execute trigger is already set!");
		this.executeTrigger = executeTrigger;
	}

	@Override
	public boolean preInit() {
		return core.preInit() && super.preInit();
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		return core.init(expressions, matchedPattern, parseResult);
	}

	@Override
	protected void execute(Event event) {
		assert executeTrigger != null;

		EffectTriggerEvent triggerEvent = new EffectTriggerEvent(
			event,
			core.expressions(),
			core.matchedPattern(),
			core.parseResult(),
			getNext()
		);

		if (core.parseEvent() == null) {
			TriggerItem.walk(executeTrigger, triggerEvent);
			return;
		}
		Variables.withLocalVariables(core.parseEvent(), triggerEvent,
			() -> TriggerItem.walk(executeTrigger, triggerEvent));
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return core.usedPattern();
	}

}
