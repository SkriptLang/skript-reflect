package org.skriptlang.reflect.syntax.custom.condition;

import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.CustomSyntaxModule;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxInfo;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.function.Predicate;

public class CustomConditionInfo extends CustomSyntaxInfo<CustomCondition> {

	private final SyntaxInfo<CustomCondition> info;
	private final boolean property;
	private Trigger checkTrigger;

	public CustomConditionInfo(
		String[] patterns,
		boolean hasParseSection,
		@Nullable Script script,
		Predicate<ParserInstance> usableInPredicate,
		boolean property
	) {
		super(patterns, hasParseSection, script, usableInPredicate);
		this.property = property;
		this.info = SyntaxInfo.builder(CustomCondition.class)
			.origin(CustomSyntaxModule.ORIGIN)
			.supplier(this::newInstance)
			.addPatterns(patterns)
			.priority(priority())
			.build();
	}

	public boolean property() {
		return property;
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
	public boolean register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.CONDITION, info);
		return true;
	}

	@Override
	public boolean unregister(SyntaxRegistry registry) {
		registry.unregister(SyntaxRegistry.CONDITION, info);
		return true;
	}

	@Override
	public CustomCondition newInstance() {
		return new CustomCondition(this);
	}

}
