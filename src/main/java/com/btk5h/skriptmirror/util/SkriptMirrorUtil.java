package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;
import ch.njol.skript.util.Utils;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.Null;
import com.btk5h.skriptmirror.ObjectWrapper;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SkriptMirrorUtil {

	/**
	 * A word ($ also an allowed char) that doesn't start with a digit.
	 */
	public static final String IDENTIFIER = "[_a-zA-Z$][\\w$]*";
	/**
	 * A full classname (e.g. java.lang.String)
	 */
	public static final String PACKAGE = "(?:" + IDENTIFIER + "\\.)*(?:" + IDENTIFIER + ")";

	private static final Pattern TYPE_PREFIXES = Pattern.compile("^[-*~]*");

	/**
	 * @return the {@link Class} of this {@link JavaType}, if it is a {@link JavaType},
	 * otherwise it returns {@link #getClass(Object)} of this object.
	 */
	public static Class<?> toClassUnwrapJavaTypes(Object o) {
		if (o instanceof JavaType) {
			return ((JavaType) o).getJavaClass();
		}

		return getClass(o);
	}

	public static String getDebugName(Class<?> cls) {
		return Skript.logVeryHigh() ? cls.getName() : cls.getSimpleName();
	}

	/**
	 * The given object is first possibly unwrapped using {@link ObjectWrapper#unwrapIfNecessary(Object)}.
	 * Then returns the class of the object, or {@code Object.class}, if the object is {@code null}.
	 */
	public static Class<?> getClass(Object o) {
		o = ObjectWrapper.unwrapIfNecessary(o);

		if (o == null) {
			return Object.class;
		}

		return o.getClass();
	}

	/**
	 * Returns the given pattern, with all types starting with an underscore
	 * replaced with {@code javaobject(s)}, as those types are just context indicators.
	 */
	public static String preprocessPattern(String pattern) {
		StringBuilder newPattern = new StringBuilder(pattern.length());
		String[] parts = pattern.split("%");

		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (i % 2 == 0) {
				newPattern.append(part);
			} else {
				if (part.startsWith("_")) {
					part = Utils.getEnglishPlural(part).getSecond() ? "javaobjects" : "javaobject";
				} else {
					part = processTypes(part);
				}

				newPattern.append('%');
				newPattern.append(part);
				newPattern.append('%');
			}
		}

		return newPattern.toString();
	}

	/**
	 * Returns the given type string, using {@link SkriptUtil#replaceUserInputPatterns(String)}
	 * on each type in the given string.
	 */
	public static String processTypes(String part) {
		if (part.length() > 0) {
			// copy all prefixes
			String prefixes = "";
			Matcher prefixMatcher = TYPE_PREFIXES.matcher(part);
			if (prefixMatcher.find()) {
				prefixes = prefixMatcher.group();
			}
			part = part.substring(prefixes.length());

			// copy all suffixes
			String suffixes = "";
			int timeIndex = part.indexOf("@");
			if (timeIndex != -1) {
				suffixes = part.substring(timeIndex);
				part = part.substring(0, timeIndex);
			}

			// replace user input patterns
			String types = Arrays.stream(part.split("/"))
				.map(SkriptUtil::replaceUserInputPatterns)
				.collect(Collectors.joining("/"));

			return prefixes + types + suffixes;
		}
		return part;
	}

	/**
	 * @return {@link Null#getInstance()} if the given object is {@code null},
	 * otherwise returns the given object itself.
	 */
	public static Object reifyIfNull(Object o) {
		return o == null ? Null.getInstance() : o;
	}

}
