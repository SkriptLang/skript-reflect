package org.skriptlang.reflect.syntax.custom.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.custom.event.CustomEventManager;
import org.skriptlang.reflect.syntax.custom.event.CustomEventManager.EventEntry;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprCustomEvent extends SimpleExpression<Event> {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprCustomEvent.class, Event.class)
			.supplier(ExprCustomEvent::new)
			.addPattern("[a] [new] custom event %string%")
			.addPattern("[a] [new] custom event %string% (with|using) data %~objects%")
			.addPattern("[a] [new] custom event %string% (with|using) [[event-]values] %~objects% [and [with|using] data %-~objects%]")
			.build());
	}

	private Expression<String> name;
	private Variable<?> eventValues, dataValues;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		//noinspection unchecked
		name = (Expression<String>) expressions[0];
		if (expressions.length == 1)
			return true;

		if (!(expressions[1] instanceof Variable<?> var1) || !var1.isList()) {
			Skript.error(expressions[1] + " is not a list variable.");
			return false;
		}

		if (expressions.length == 3 && expressions[2] != null && (!(expressions[2] instanceof Variable<?> var2) || !var2.isList())) {
			Skript.error(expressions[2] + " is not a list variable.");
			return false;
		}

		if (matchedPattern == 1) {
			dataValues = var1;
		} else {
			eventValues = var1;
			eventValues.returnNestedStructures(true);
			dataValues = (Variable<?>) expressions[1];
		}

		return true;
	}

	@Override
	protected Event @Nullable [] get(Event event) {
		String name = this.name.getSingle(event);
		EventEntry eventEntry = CustomEventManager.getEvent(name);
		if (eventEntry == null)
			return new Event[0];
		BukkitCustomEvent customEvent = eventEntry.newInstance();

		if (eventValues != null)
			customEvent.setEventValues(event, eventValues, this::error);

		if (dataValues != null) {
			dataValues.keyedIterator(event).forEachRemaining(keyedValue ->
				customEvent.setData(keyedValue.key(), keyedValue.value()));
		}

		return new Event[] {customEvent};
	}

	@Override
	public Class<? extends Event> getReturnType() {
		return BukkitCustomEvent.class;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		builder.append("custom event", name);
		if (eventValues != null)
			builder.append("with event-values", eventValues);
		if (eventValues != null && dataValues != null)
			builder.append("and");
		if (dataValues != null)
			builder.append("with data", dataValues);
		return builder.toString();
	}

}
