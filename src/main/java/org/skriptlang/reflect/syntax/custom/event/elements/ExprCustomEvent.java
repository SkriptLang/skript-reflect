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
		if (matchedPattern == 0)
			return true;

		Expression<?> eventValues = null, dataValues;
		if (matchedPattern == 1) {
			dataValues = expressions[1];
		} else {
			eventValues = expressions[1];
			dataValues = expressions[2];
		}

		if (eventValues != null && (!(eventValues instanceof Variable<?> variable) || !variable.isList())) {
			Skript.error(eventValues + " is not a list variable.");
			return false;
		}

		if (dataValues != null && (!(dataValues instanceof Variable<?> variable) || !variable.isList())) {
			Skript.error(dataValues + " is not a list variable.");
			return false;
		}

		this.eventValues = (Variable<?>) eventValues;
		if (this.eventValues != null)
			this.returnNestedStructures(true);

		this.dataValues = (Variable<?>) dataValues;

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
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Event> getReturnType() {
		return BukkitCustomEvent.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		builder.append("custom event", name)
			.appendIf(eventValues != null, "with event-values", eventValues)
			.appendIf(eventValues != null && dataValues != null, "and")
			.appendIf(dataValues != null, "with data", dataValues);
		return builder.toString();
	}

}
