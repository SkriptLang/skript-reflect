package org.skriptlang.reflect.syntax.custom.expression;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntax;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxCore;

@NoDoc
public class CustomExpression<T> extends SimpleExpression<T> implements CustomSyntax {

	private final CustomExpressionInfo<T> info;
	private final CustomSyntaxCore core;

	public CustomExpression(CustomExpressionInfo<T> info) {
		this.info = info;
		this.core = new CustomSyntaxCore(info);
	}

	@Override
	public CustomExpressionInfo<T> info() {
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
	protected T[] get(Event event) {
		assert info.getterTrigger() != null;

		ExpressionGetEvent getEvent = new ExpressionGetEvent(
			event,
			this,
			core.expressions(),
			core.matchedPattern(),
			core.parseResult()
		);

		TriggerItem.walk(info.getterTrigger(), getEvent);
		//noinspection unchecked
		return (T[]) getEvent.output();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
		return info.changerTrigger(mode).acceptedClasses();
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
		Trigger trigger = info.changerTrigger(mode).trigger();
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
	public Class<? extends T> getReturnType() {
		return info.returnType();
	}

	@Override
	public boolean isSingle() {
		if (!info.property() || !info.single())
			return info.single();
		return core.expressions()[core.matchedPattern() == 1 ? 0 : core.expressions().length - 1].isSingle();
	}

	@Override
	public boolean isLoopOf(String input) {
		return info.isLoopOf(input);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return core.usedPattern();
	}

}
