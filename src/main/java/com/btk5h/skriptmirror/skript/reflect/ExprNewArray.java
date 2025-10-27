package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.util.JavaTypeWrapper;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

public class ExprNewArray extends SimpleExpression<ObjectWrapper> {

	static {
		Skript.registerExpression(ExprNewArray.class, ObjectWrapper.class, ExpressionType.COMBINED,
			"new (<(" + JavaTypeWrapper.PRIMITIVE_PATTERNS + ")>|%-javatype%)\\[%number%\\]");
	}

	private JavaTypeWrapper javaTypeWrapper;
	private Expression<? extends Number> sizeExpression;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		javaTypeWrapper = JavaTypeWrapper.of(exprs[0], parseResult.regexes);
		sizeExpression = (Expression<? extends Number>) exprs[1];
		return true;
	}

	@Override
	@Nullable
	protected ObjectWrapper[] get(Event e) {
		JavaType javaType = javaTypeWrapper.get(e);
		Number length = sizeExpression.getSingle(e);

		if (javaType == null || length == null)
			return null;

		int size = length.intValue();
		Class<?> clazz = javaType.getJavaClass();

		Object array = Array.newInstance(clazz, size);
		return new ObjectWrapper[] {ObjectWrapper.create(array)};
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
		return "new " + javaTypeWrapper.toString(e, debug) + "[" + sizeExpression.toString(e, debug) + "]";
	}

}
