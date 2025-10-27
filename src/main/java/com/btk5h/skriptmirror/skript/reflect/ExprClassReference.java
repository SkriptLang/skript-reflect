package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.util.JavaTypeWrapper;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprClassReference extends SimpleExpression<ObjectWrapper> {

	static {
		Skript.registerExpression(ExprClassReference.class, ObjectWrapper.class, ExpressionType.COMBINED,
			"(<(" + JavaTypeWrapper.PRIMITIVE_PATTERNS + ")>|%-javatype%).class");
	}

	private JavaTypeWrapper javaTypeWrapper;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		javaTypeWrapper = JavaTypeWrapper.of(exprs[0], parseResult.regexes);
		return true;
	}

	@Nullable
	@Override
	protected ObjectWrapper[] get(Event e) {
		JavaType javaType = javaTypeWrapper.get(e);
		if (javaType == null) {
			return null;
		}
		return new ObjectWrapper[] {ObjectWrapper.create(javaType.getJavaClass())};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends ObjectWrapper> getReturnType() {
		return ObjectWrapper.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return javaTypeWrapper.toString(e, debug) + ".class";
	}

}
