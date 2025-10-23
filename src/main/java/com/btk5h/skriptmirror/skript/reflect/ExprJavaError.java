package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

public class ExprJavaError extends SimpleExpression<Throwable> {
	static {
		Skript.registerExpression(ExprJavaError.class, Throwable.class, ExpressionType.SIMPLE,
			"[the] [last] [java] (throwable|exception|error)");
	}

	@Override
	protected Throwable[] get(Event e) {
		return new Throwable[]{ExprJavaCall.lastError};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Throwable> getReturnType() {
		return Throwable.class;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return "last java error";
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		return true;
	}
}
