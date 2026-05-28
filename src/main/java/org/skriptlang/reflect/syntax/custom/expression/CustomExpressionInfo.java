package org.skriptlang.reflect.syntax.custom.expression;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ReturnHandler;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.CustomSyntaxModule;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxInfo;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Map;
import java.util.function.Predicate;

public class CustomExpressionInfo<T> extends CustomSyntaxInfo<CustomExpression<T>> implements ReturnHandler<T> {

	private final SyntaxInfo.Expression<CustomExpression<T>, ? extends T> info;
	private final Class<? extends T> returnType;
	private final boolean single, property;
	private final @Nullable String loopOf;
	private final Map<Changer.ChangeMode, ChangerTrigger> changeModes;
	private Trigger getterTrigger;

	public CustomExpressionInfo(String[] patterns, boolean hasParseSection, @Nullable Script script, Predicate<ParserInstance> usableInPredicate, Class<? extends T> returnType, boolean single, boolean property, @Nullable String loopOf, Map<Changer.ChangeMode, ChangerTrigger> changeModes) {
		super(patterns, hasParseSection, script, usableInPredicate);
		this.returnType = returnType;
		this.single = single;
		this.property = property;
		this.loopOf = loopOf;
		this.changeModes = changeModes;
		//noinspection unchecked,rawtypes
		this.info = (SyntaxInfo.Expression) SyntaxInfo.Expression.builder(CustomExpression.class, returnType)
			.addPatterns(patterns)
			.supplier(this::newInstance)
			.priority(priority())
			.build();
	}

	public Class<? extends T> returnType() {
		return returnType;
	}

	public boolean single() {
		return single;
	}

	public boolean property() {
		return property;
	}

	public @Nullable String loopOf() {
		return loopOf;
	}

	public boolean isLoopOf(String input) {
		return loopOf != null && loopOf.equalsIgnoreCase(input);
	}

	public Trigger getterTrigger() {
		return getterTrigger;
	}

	public void getterTrigger(Trigger getterTrigger) {
		if (this.getterTrigger != null)
			throw new IllegalStateException("Getter trigger is already set!");
		this.getterTrigger = getterTrigger;
	}

	public ChangerTrigger changerTrigger(Changer.ChangeMode mode) {
		return changeModes.get(mode);
	}

	public void changerTrigger(Changer.ChangeMode mode, Trigger changerTrigger, Class<?>[] acceptedClasses) {
		if (changeModes.get(mode) != null)
			throw new IllegalStateException(mode.name() + " trigger is already set!");
		changeModes.put(mode, new ChangerTrigger(changerTrigger, acceptedClasses));
	}

	@Override
	public boolean register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, info);
		return true;
	}

	@Override
	public boolean unregister(SyntaxRegistry registry) {
		registry.unregister(SyntaxRegistry.EXPRESSION, info);
		return true;
	}

	@Override
	public CustomExpression<T> newInstance() {
		return new CustomExpression<>(this);
	}

	@Override
	public void returnValues(Event event, Expression<? extends T> value) {
		assert event instanceof ExpressionGetEvent;
		((ExpressionGetEvent) event).output(value.getArray(event));
	}

	@Override
	public boolean isSingleReturnValue() {
		return single && !property;
	}

	@Override
	public @Nullable Class<? extends T> returnValueType() {
		return returnType();
	}

}
