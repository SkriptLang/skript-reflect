package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

public class CustomSyntaxExpression extends SimpleExpression<Object> {
	private final Expression<?> source;
	private final Event realEvent;
	private final Object[] value;

	public CustomSyntaxExpression(Expression<?> source, Event realEvent) {
		this.source = source;
		this.realEvent = realEvent;
		this.value = source == null ? new Object[0] : source.getAll(realEvent);
	}

	public static CustomSyntaxExpression wrap(Expression<?> source, Event realEvent) {
		if (source instanceof CustomSyntaxExpression) {
			return (CustomSyntaxExpression) source;
		}
		return new CustomSyntaxExpression(source, realEvent);
	}

	@Override
	protected Object[] get(Event e) {
		return value;
	}

	@Override
	public boolean isSingle() {
		return source == null || source.isSingle();
	}

	@Override
	public Class<?> getReturnType() {
		return source == null ? Object.class : source.getReturnType();
	}

	@Override
	public String toString(Event e, boolean debug) {
		return source == null ? "" : source.toString(realEvent, debug);
	}

	@Override
	public Expression<?> getSource() {
		return source;
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		throw new UnsupportedOperationException();
	}

}
