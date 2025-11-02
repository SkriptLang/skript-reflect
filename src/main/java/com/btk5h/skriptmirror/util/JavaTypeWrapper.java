package com.btk5h.skriptmirror.util;

import ch.njol.skript.lang.Expression;
import com.btk5h.skriptmirror.JavaType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.MatchResult;

/**
 * Hold either a {@link Class} object, or an Expression that can retrieve a {@link JavaType}.
 */
@SuppressWarnings("ConstantConditions")
public class JavaTypeWrapper {

	/**
	 * Contains a string of all primitive types (excluding void), separated by {@code |}.
	 */
	public static final String PRIMITIVE_PATTERNS = String.join("|", JavaUtil.PRIMITIVE_CLASS_NAMES.keySet());

	private final Expression<? extends JavaType> javaTypeExpression;
	private final Class<?> clazz;

	public JavaTypeWrapper(@Nullable Expression<? extends JavaType> javaTypeExpression, @Nullable Class<?> clazz) {
		this.javaTypeExpression = javaTypeExpression;
		this.clazz = clazz;
	}

	public static JavaTypeWrapper ofExpression(Expression<? extends JavaType> javaTypeExpression) {
		return new JavaTypeWrapper(javaTypeExpression, null);
	}

	public static JavaTypeWrapper ofClass(Class<?> clazz) {
		return new JavaTypeWrapper(null, clazz);
	}

	@SuppressWarnings("unchecked")
	public static JavaTypeWrapper of(Expression<?> expression, List<MatchResult> matchResults) {
		if (expression != null) {
			return ofExpression((Expression<? extends JavaType>) expression);
		} else {
			String primitiveType = matchResults.get(0).group();
			return ofClass(JavaUtil.PRIMITIVE_CLASS_NAMES.get(primitiveType));
		}
	}

	public JavaType get(Event event) {
		if (clazz != null) {
			return new JavaType(clazz);
		} else {
			return javaTypeExpression.getSingle(event);
		}
	}

	public String toString(@Nullable Event event, boolean debug) {
		if (clazz != null) {
			return clazz.getName();
		} else {
			return javaTypeExpression.toString(event, debug);
		}
	}

	@Override
	public String toString() {
		if (clazz != null) {
			return clazz.getName();
		} else {
			return javaTypeExpression.toString(null, false);
		}
	}

}
