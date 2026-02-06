package org.skriptlang.reflect.syntax.custom.expression;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ReturnHandler;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleExpression;
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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

public class CustomExpression<T> extends SimpleExpression<T>
	implements CustomSyntax<SyntaxInfo.Expression<?, ?>>, ReturnHandler<T> {

	private final CustomSyntaxCore core;
	private final SyntaxInfo.Expression<CustomExpression<T>, ? extends T> info;
	private final Class<? extends T> returnType;
	private final boolean single, property;
	private final @Nullable String loopOf;
	private final Map<ChangeMode, ChangerTrigger> changeModes;
	private Trigger getterTrigger;


	public CustomExpression(
		String[] patterns,
		boolean hasParseSection,
		@Nullable Script script,
		Predicate<ParserInstance> usableInPredicate,
		Class<? extends T> returnType,
		boolean single,
		boolean property,
		@Nullable String loopOf,
		Map<ChangeMode, ChangerTrigger> changeModes
	) {
		this(
			new CustomSyntaxCore(
				patterns,
				hasParseSection,
				script,
				usableInPredicate
			),
			returnType,
			single,
			property,
			loopOf,
			changeModes,
			null
		);
	}

	public CustomExpression(
		CustomSyntaxCore core,
		Class<? extends T> returnType,
		boolean single,
		boolean property,
		@Nullable String loopOf,
		Map<ChangeMode, ChangerTrigger> changeModes,
		@Nullable Trigger getterTrigger
	) {
		this.core = core;
		this.returnType = returnType;
		this.single = single;
		this.property = property;
		this.loopOf = loopOf;
		this.changeModes = changeModes;
		this.getterTrigger = getterTrigger;
		//noinspection unchecked,rawtypes
		this.info = (SyntaxInfo.Expression) SyntaxInfo.Expression.builder(CustomExpression.class, returnType)
			.origin(Origin.of(SkriptMirror.getAddonInstance()))
			.supplier(this::copy)
			.addPatterns(core.patterns())
			.priority(core.priority())
			.build();
	}

	@Override
	public SyntaxRegistry.Key<SyntaxInfo.Expression<?, ?>> key() {
		return SyntaxRegistry.EXPRESSION;
	}

	@Override
	public SyntaxInfo.Expression<?, ?> info() {
		return info;
	}

	@Override
	public CustomExpression<T> copy() {
		return new CustomExpression<>(core.copy(), returnType, single, property, loopOf, changeModes, getterTrigger);
	}

	@Override
	public Trigger parseTrigger() {
		return core.parseTrigger();
	}

	@Override
	public void parseTrigger(Trigger parseTrigger) {
		core.parseTrigger(parseTrigger);
	}

	public Trigger getterTrigger() {
		return getterTrigger;
	}

	public void getterTrigger(Trigger getterTrigger) {
		if (this.getterTrigger != null)
			throw new IllegalStateException("Get trigger is already set!");
		this.getterTrigger = getterTrigger;
	}

	public ChangerTrigger changerTrigger(ChangeMode mode) {
		return changeModes.get(mode);
	}

	public void changerTrigger(ChangeMode mode, Trigger changerTrigger, Class<?>[] acceptedClasses) {
		if (changeModes.get(mode) != null)
			throw new IllegalStateException(mode.name() + " trigger is already set!");
		changeModes.put(mode, new ChangerTrigger(changerTrigger, acceptedClasses));
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
	protected T[] get(Event event) {
		assert getterTrigger != null;

		ExpressionGetEvent getEvent = new ExpressionGetEvent(
			event,
			this,
			core.expressions(),
			core.matchedPattern(),
			core.parseResult()
		);

		TriggerItem.walk(getterTrigger, getEvent);
		//noinspection unchecked
		return (T[]) getEvent.output();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
		return changerTrigger(mode).acceptedClasses();
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
		Trigger trigger = changerTrigger(mode).trigger();
		ExpressionChangeEvent changeEvent = new ExpressionChangeEvent(
			event,
			this,
			core.expressions(),
			core.matchedPattern(),
			core.parseResult(),
			delta
		);
		Trigger.walk(trigger, changeEvent);
	}

	@Override
	public void returnValues(Event event, Expression<? extends T> value) {
		assert event instanceof ExpressionGetEvent;
		((ExpressionGetEvent) event).output(value.getArray(event));
	}

	@Override
	public Class<? extends T> getReturnType() {
		return returnType;
	}

	@Override
	public boolean isSingle() {
		if (!property || !single)
			return single;
		return core.expressions()[core.matchedPattern() == 1 ? 0 : core.expressions().length - 1].isSingle();
	}

	@Override
	public boolean isSingleReturnValue() {
		return single;
	}

	@Override
	public @Nullable Class<? extends T> returnValueType() {
		return getReturnType();
	}

	@Override
	public boolean isLoopOf(String input) {
		return loopOf != null && loopOf.equalsIgnoreCase(input);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return core.usedPattern();
	}

}
