package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.FunctionWrapper;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.util.Arrays;

public class ExprFunction extends SimpleExpression<FunctionWrapper> {
	static {
		Skript.registerExpression(ExprFunction.class, FunctionWrapper.class, ExpressionType.PROPERTY,
			"[the] function(s| [reference[s]]) %strings% [called with [[the] [arg[ument][s]]] %-objects%]");
	}

	private Expression<String> refs;
	private Expression<Object> args;

	@Override
	protected FunctionWrapper[] get(Event e) {
		Object[] functionArgs = args == null ? new Object[0] : args.getArray(e);

		return Arrays.stream(refs.getArray(e))
			.map(ref -> new FunctionWrapper(ref, functionArgs))
			.toArray(FunctionWrapper[]::new);
	}

	@Override
	public boolean isSingle() {
		return refs.isSingle();
	}

	@Override
	public Class<? extends FunctionWrapper> getReturnType() {
		return FunctionWrapper.class;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return "function reference of " + refs.toString(e, debug);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		refs = SkriptUtil.defendExpression(exprs[0]);

		if (exprs[1] != null) {
			args = SkriptUtil.defendExpression(exprs[1]);
		}

		return SkriptUtil.canInitSafely(args);
	}
}
