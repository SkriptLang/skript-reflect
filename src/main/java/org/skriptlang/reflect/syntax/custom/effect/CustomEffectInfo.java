package org.skriptlang.reflect.syntax.custom.effect;

import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.CustomSyntaxModule;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxInfo;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.function.Predicate;

public class CustomEffectInfo extends CustomSyntaxInfo<CustomEffect> {

	private final SyntaxInfo<CustomEffect> info;
	private @Nullable Trigger executeTrigger;

	public CustomEffectInfo(String[] patterns, boolean hasParseSection, @Nullable Script script, Predicate<ParserInstance> usableInPredicate) {
		super(patterns, hasParseSection, script, usableInPredicate);
		this.info = SyntaxInfo.builder(CustomEffect.class)
			.origin(CustomSyntaxModule.ORIGIN)
			.addPatterns(patterns)
			.supplier(this::newInstance)
			.priority(priority())
			.build();
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
	public boolean register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EFFECT, info);
		return true;
	}

	@Override
	public boolean unregister(SyntaxRegistry registry) {
		registry.unregister(SyntaxRegistry.EFFECT, info);
		return true;
	}

	@Override
	public CustomEffect newInstance() {
		return new CustomEffect(this);
	}

}
