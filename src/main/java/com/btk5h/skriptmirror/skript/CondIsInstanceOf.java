package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

public class CondIsInstanceOf extends Condition {
	static {
		Skript.registerCondition(CondIsInstanceOf.class,
			"%objects% (is|are) [a[n]] instance[s] of %javatypes%",
			"%objects% (is not|isn't|are not|aren't) [a[n]] instance[s] of %javatypes%");
	}

	private Expression<Object> objects;
	private Expression<JavaType> type;

	@Override
	public boolean check(Event e) {
		return objects.check(e, o ->
			type.check(e, t ->
					t.getJavaClass().isAssignableFrom(SkriptMirrorUtil.getClass(o)),
				isNegated()
			)
		);
	}

	@Override
	public String toString(Event e, boolean debug) {
		return String.format("%s instanceof %s", objects.toString(e, debug), type.toString(e, debug));
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		objects = SkriptUtil.defendExpression(exprs[0]);
		type = SkriptUtil.defendExpression(exprs[1]);
		setNegated(matchedPattern == 1);
		return true;
	}
}
