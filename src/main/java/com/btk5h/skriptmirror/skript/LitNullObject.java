package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.Null;
import org.bukkit.event.Event;

public class LitNullObject extends SimpleLiteral<Null> {
	static {
		Skript.registerExpression(LitNullObject.class, Null.class, ExpressionType.SIMPLE, "null");
	}

	public LitNullObject() {
		super(Null.getInstance(), false);
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return "null";
	}
}
