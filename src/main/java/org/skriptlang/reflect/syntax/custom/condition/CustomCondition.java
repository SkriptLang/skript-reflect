package org.skriptlang.reflect.syntax.custom.condition;

import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntax;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxCore;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.function.Predicate;

public class CustomCondition extends Condition implements CustomSyntax<SyntaxInfo<? extends Condition>> {

	private final CustomSyntaxCore core;
	private final boolean property;
	private final SyntaxInfo<CustomCondition> info;
	private Trigger checkTrigger;

	public CustomCondition(
		String[] patterns,
		boolean hasParseSection,
		@Nullable Script script,
		Predicate<ParserInstance> usableInPredicate,
		boolean property
	) {
		this(
			new CustomSyntaxCore(
				patterns,
				hasParseSection,
				script,
				usableInPredicate
			),
			property,
			null
		);
	}

	public CustomCondition(CustomSyntaxCore core, boolean property, @Nullable Trigger checkTrigger) {
		this.core = core;
		this.property = property;
		this.checkTrigger = checkTrigger;
		this.info = SyntaxInfo.builder(CustomCondition.class)
			.origin(Origin.of(SkriptMirror.getAddonInstance()))
			.supplier(this::copy)
			.addPatterns(core.patterns())
			.priority(core.priority())
			.build();
	}

	@Override
	public SyntaxRegistry.Key<SyntaxInfo<? extends Condition>> key() {
		return SyntaxRegistry.CONDITION;
	}

	@Override
	public SyntaxInfo<? extends Condition> info() {
		return info;
	}

	@Override
	public CustomCondition copy() {
		return new CustomCondition(core.copy(), property, checkTrigger);
	}

	@Override
	public Trigger parseTrigger() {
		return core.parseTrigger();
	}

	@Override
	public void parseTrigger(Trigger parseTrigger) {
		core.parseTrigger(parseTrigger);
	}

	public Trigger checkTrigger() {
		return checkTrigger;
	}

	public void checkTrigger(Trigger checkTrigger) {
		if (this.checkTrigger != null)
			throw new IllegalStateException("Check trigger is already set!");
		this.checkTrigger = checkTrigger;
	}

	@Override
	public boolean preInit() {
		return core.preInit() && super.preInit();
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!core.init(this, expressions, matchedPattern, parseResult))
			return false;
		if (property)
			setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event event) {
		assert checkTrigger != null;

		ConditionCheckEvent checkEvent = new ConditionCheckEvent(
			event,
			this,
			core.expressions(),
			core.matchedPattern(),
			core.parseResult()
		);

		if (core.parseEvent() == null) {
			Trigger.walk(checkTrigger, checkEvent);
		} else {
			Variables.withLocalVariables(core.parseEvent(), checkEvent, () -> Trigger.walk(checkTrigger, checkEvent));
		}
		return checkEvent.isMarkedContinue() ^ checkEvent.isNegated() ^ isNegated();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return core.usedPattern();
	}

}
