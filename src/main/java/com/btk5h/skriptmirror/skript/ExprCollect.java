package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.Null;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ExprCollect extends SimpleExpression<ObjectWrapper> {
	static {
		Skript.registerExpression(ExprCollect.class, ObjectWrapper.class, ExpressionType.COMBINED,
			"\\[%objects%[ as %-javatype%[ ]]\\]");
	}

	private Expression<Object> objects;
	private Expression<JavaType> type;

	@Override
	protected ObjectWrapper[] get(Event e) {
		Stream<Object> objectStream = Arrays.stream(objects.getArray(e))
			.map(o -> o instanceof Null ? null : o)
			.map(ObjectWrapper::unwrapIfNecessary);

		if (type != null) {
			JavaType componentType = type.getSingle(e);
			if (componentType != null) {
				objectStream = objectStream.filter(o -> o == null || componentType.getJavaClass().isInstance(o));
			}
		}

		Object[] items = objectStream.toArray();
		Object[] castedItems = JavaUtil.newArray(getCommonSuperclass(items), items.length);

		System.arraycopy(items, 0, castedItems, 0, items.length);

		return new ObjectWrapper[]{ObjectWrapper.create(castedItems)};
	}

	private static Class<?> getCommonSuperclass(Object[] objects) {
		Optional<Object> firstNonnull = Arrays.stream(objects)
			.filter(Objects::nonNull)
			.findFirst();

		if (firstNonnull.isPresent()) {
			return Arrays.stream(objects)
				.filter(Objects::nonNull)
				.map(Object::getClass)
				.map(o -> (Class) o)
				.reduce(firstNonnull.get().getClass(), ExprCollect::getCommonSuperclass);
		}

		return Object.class;
	}

	private static Class<?> getCommonSuperclass(Class<?> c1, Class<?> c2) {
		while (!c1.isAssignableFrom(c2)) {
			c1 = c1.getSuperclass();
		}
		return c1;
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
	public String toString(Event e, boolean debug) {
		return "array of " + objects.toString(e, debug);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		objects = SkriptUtil.defendExpression(exprs[0]);
		type = (Expression<JavaType>) exprs[1];
		return SkriptUtil.canInitSafely(objects);
	}
}
