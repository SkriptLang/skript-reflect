package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import org.bukkit.event.Event;

import java.util.Arrays;

public class ExprEventClasses extends SimpleExpression<JavaType> {
	static {
		Skript.registerExpression(ExprEventClasses.class, JavaType.class, ExpressionType.SIMPLE, "event-classes");
	}

	@Override
	protected JavaType[] get(Event e) {
		return Arrays.stream(((SyntaxParseEvent) e).getEventClasses())
			.map(JavaType::new)
			.toArray(JavaType[]::new);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends JavaType> getReturnType() {
		return JavaType.class;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return "event-classes";
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		return getParser().isCurrentEvent(SyntaxParseEvent.class);
	}

}
