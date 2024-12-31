package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

public class ExprMemberNames extends SimpleExpression<String> {
	static {
		PropertyExpression.register(ExprMemberNames.class, String.class, "(0¦field|1¦method) names", "objects");
	}

	private Expression<Object> target;
	private Function<Class<?>, Stream<? extends Member>> mapper;

	@Override
	protected String[] get(Event e) {
		return Arrays.stream(target.getArray(e))
			.map(SkriptMirrorUtil::toClassUnwrapJavaTypes)
			.flatMap(mapper)
			.map(Member::getName)
			.distinct()
			.toArray(String[]::new);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return "member names of " + target.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		target = SkriptUtil.defendExpression(exprs[0]);

		switch (parseResult.mark) {
			case 0:
				mapper = JavaUtil::fields;
				break;
			case 1:
				mapper = JavaUtil::methods;
				break;
		}

		return SkriptUtil.canInitSafely(target);
	}
}
