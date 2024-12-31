package com.btk5h.skriptmirror;

import org.skriptlang.reflect.java.elements.structures.StructImport;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import org.skriptlang.skript.lang.script.Script;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Descriptor is all the info that can be provided in a script about a method.
 * This consists of:
 * <ul>
 *   <li>An optional class the member (method, field or constructor) is defined in.</li>
 *   <li>The name of the member</li>
 *   <li>The parameter types of the member.</li>
 * </ul>
 */
public final class Descriptor {

	/**
	 * A regex string for a list of array symbols, e.g. {@code [][][]}.
	 */
	private static final String PACKAGE_ARRAY = SkriptMirrorUtil.PACKAGE + "(?:\\[])*";

	/**
	 * A regex {@link Pattern} for a single array symbol, matches only {@code []}.
	 */
	private static final Pattern PACKAGE_ARRAY_SINGLE = Pattern.compile("\\[]");

	/**
	 * A regex {@link Pattern} for a {@link Descriptor}.
	 */
	private static final Pattern DESCRIPTOR =
		Pattern.compile("" +
			"(?:\\[(" + SkriptMirrorUtil.PACKAGE + ")])?" +
			"(" + SkriptMirrorUtil.IDENTIFIER + ")" +
			"(?:\\[((?:" + PACKAGE_ARRAY + "\\s*,\\s*)*(?:" + PACKAGE_ARRAY + "))])?"
		);

	private final Class<?> javaClass;
	private final String name;
	private final Class<?>[] parameterTypes;

	public Descriptor(Class<?> javaClass, String name, Class<?>[] parameterTypes) {
		this.javaClass = javaClass;
		this.name = name;
		this.parameterTypes = parameterTypes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Descriptor that = (Descriptor) o;
		return Objects.equals(javaClass, that.javaClass) &&
			Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(javaClass, name);
	}

	public Class<?> getJavaClass() {
		return javaClass;
	}

	public String getName() {
		return name;
	}

	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean isStatic) {
		return javaClass == null ? "(unspecified)" : SkriptMirrorUtil.getDebugName(javaClass)
			+ (isStatic ? "." : "#")
			+ name;
	}

	/**
	 * Returns a new descriptor with a new {@link #javaClass}.
	 * If this Descriptors {@link #javaClass} is not null, it will instead return itself.
	 */
	public Descriptor orDefaultClass(Class<?> cls) {
		if (javaClass != null) {
			return this;
		}

		return new Descriptor(cls, name, parameterTypes);
	}

	/**
	 * Parses the given {@link String} as a {@link Descriptor}. The script parameter is to get the imports.
	 */
	public static Descriptor parse(String desc, Script script) throws ImportNotFoundException {
		Matcher m = DESCRIPTOR.matcher(desc);

		if (m.matches()) {
			String cls = m.group(1);
			Class<?> javaClass = cls == null ? null : lookupClass(script, cls);

			String name = m.group(2);

			String args = m.group(3);
			Class<?>[] parameterTypes = args == null ? null : parseParams(args, script);

			return new Descriptor(javaClass, name, parameterTypes);
		}

		return null;
	}

	/**
	 * Parses a list of imported names, returning a class array containing the classes in the given string.
	 */
	private static Class<?>[] parseParams(String args, Script script) throws ImportNotFoundException {
		String[] rawClasses = args.split(",");

		Class<?>[] parsedClasses = new Class<?>[rawClasses.length];

		for (int i = 0; i < rawClasses.length; i++) {
			String userType = rawClasses[i].trim();

			// Calculate array depth with regex
			Matcher arrayDepthMatcher = PACKAGE_ARRAY_SINGLE.matcher(userType);
			int arrayDepth = 0;
			while (arrayDepthMatcher.find()) {
				arrayDepth++;
			}

			// Remove array's square brackets
			userType = userType.substring(0, userType.length() - (2 * arrayDepth));

			Class<?> cls;
			if (JavaUtil.PRIMITIVE_CLASS_NAMES.containsKey(userType)) {
				cls = JavaUtil.PRIMITIVE_CLASS_NAMES.get(userType);
			} else {
				cls = lookupClass(script, userType);
			}

			// Convert class to array class with calculated depth
			cls = JavaUtil.getArrayClass(cls, arrayDepth);

			parsedClasses[i] = cls;
		}

		return parsedClasses;
	}

	/**
	 * Looks up a class from its imported name in the given file.
	 */
	private static Class<?> lookupClass(Script script, String userType) throws ImportNotFoundException {
		JavaType customImport = StructImport.lookup(script, userType);
		if (customImport == null)
			throw new ImportNotFoundException(userType);

		return customImport.getJavaClass();
	}

}
