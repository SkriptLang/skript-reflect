package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.ClassInfo;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.Null;
import org.bukkit.inventory.ItemStack;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public final class JavaUtil {
	public static final Map<Class<?>, Class<?>> WRAPPER_CLASSES = new HashMap<>();
	public static final Set<Class<?>> NUMERIC_CLASSES = new HashSet<>();
	public static final Map<String, Class<?>> PRIMITIVE_CLASS_NAMES = new HashMap<>();

	static {
		WRAPPER_CLASSES.put(boolean.class, Boolean.class);
		WRAPPER_CLASSES.put(byte.class, Byte.class);
		WRAPPER_CLASSES.put(char.class, Character.class);
		WRAPPER_CLASSES.put(double.class, Double.class);
		WRAPPER_CLASSES.put(float.class, Float.class);
		WRAPPER_CLASSES.put(int.class, Integer.class);
		WRAPPER_CLASSES.put(long.class, Long.class);
		WRAPPER_CLASSES.put(short.class, Short.class);

		NUMERIC_CLASSES.add(byte.class);
		NUMERIC_CLASSES.add(double.class);
		NUMERIC_CLASSES.add(float.class);
		NUMERIC_CLASSES.add(int.class);
		NUMERIC_CLASSES.add(long.class);
		NUMERIC_CLASSES.add(short.class);
		NUMERIC_CLASSES.add(Byte.class);
		NUMERIC_CLASSES.add(Double.class);
		NUMERIC_CLASSES.add(Float.class);
		NUMERIC_CLASSES.add(Integer.class);
		NUMERIC_CLASSES.add(Long.class);
		NUMERIC_CLASSES.add(Short.class);

		PRIMITIVE_CLASS_NAMES.put("boolean", boolean.class);
		PRIMITIVE_CLASS_NAMES.put("byte", byte.class);
		PRIMITIVE_CLASS_NAMES.put("char", char.class);
		PRIMITIVE_CLASS_NAMES.put("double", double.class);
		PRIMITIVE_CLASS_NAMES.put("float", float.class);
		PRIMITIVE_CLASS_NAMES.put("int", int.class);
		PRIMITIVE_CLASS_NAMES.put("long", long.class);
		PRIMITIVE_CLASS_NAMES.put("short", short.class);
	}

	/**
	 * @return a {@link Stream} of {@link Field}s, with the fields declared in the given class,
	 * and the public fields of the given class, without duplicates.
	 */
	public static Stream<Field> fields(Class<?> cls) {
		return Stream.concat(
			Arrays.stream(cls.getFields()),
			Arrays.stream(cls.getDeclaredFields())
		).distinct();
	}

	/**
	 * @return a {@link Stream} of {@link Method}s, with the methods declared in the given class
	 * and the public methods of the given class, without duplicates.
	 */
	public static Stream<Method> methods(Class<?> cls) {
		return Stream.concat(
			Arrays.stream(cls.getMethods()),
			Arrays.stream(cls.getDeclaredMethods())
		).distinct();
	}

	/**
	 * @return a {@link Stream} of the {@link Constructor}s declared in the given class.
	 */
	public static Stream<Constructor> constructors(Class<?> cls) {
		return Arrays.stream(cls.getDeclaredConstructors());
	}

	/**
	 * Calls {@code toGenericString} on the given {@link Member},
	 * returning {@code null} if the given {@link Member} is not
	 * a {@link Field}, a {@link Method} or a {@link Constructor}.
	 */
	public static String toGenericString(Member o) {
		if (o instanceof Field) {
			return ((Field) o).toGenericString();
		} else if (o instanceof Method) {
			return ((Method) o).toGenericString();
		} else if (o instanceof Constructor) {
			return ((Constructor) o).toGenericString();
		}
		return null;
	}

	/**
	 * @return an array of the wrapper type of the component type of the given array (e.g. {@code int[] -> Integer[]}).
	 * The contents of the array are copied over.
	 */
	public static Object boxPrimitiveArray(Object obj) {
		Class<?> componentType = obj.getClass().getComponentType();
		if (componentType == null || !componentType.isPrimitive()) {
			return obj;
		}
		int length = Array.getLength(obj);
		if (componentType == int.class) {
			int[] source = (int[]) obj;
			Integer[] target = new Integer[length];
			for (int i = 0; i < length; i++) target[i] = source[i];
			return target;
		} else if (componentType == long.class) {
			long[] source = (long[]) obj;
			Long[] target = new Long[length];
			for (int i = 0; i < length; i++) target[i] = source[i];
			return target;
		} else if (componentType == double.class) {
			double[] source = (double[]) obj;
			Double[] target = new Double[length];
			for (int i = 0; i < length; i++) target[i] = source[i];
			return target;
		} else if (componentType == float.class) {
			float[] source = (float[]) obj;
			Float[] target = new Float[length];
			for (int i = 0; i < length; i++) target[i] = source[i];
			return target;
		} else if (componentType == boolean.class) {
			boolean[] source = (boolean[]) obj;
			Boolean[] target = new Boolean[length];
			for (int i = 0; i < length; i++) target[i] = source[i];
			return target;
		} else if (componentType == byte.class) {
			byte[] source = (byte[]) obj;
			Byte[] target = new Byte[length];
			for (int i = 0; i < length; i++) target[i] = source[i];
			return target;
		} else if (componentType == char.class) {
			char[] source = (char[]) obj;
			Character[] target = new Character[length];
			for (int i = 0; i < length; i++) target[i] = source[i];
			return target;
		} else if (componentType == short.class) {
			short[] source = (short[]) obj;
			Short[] target = new Short[length];
			for (int i = 0; i < length; i++) target[i] = source[i];
			return target;
		}
		return obj;
	}

	/**
	 * Converts a numeric array to a different numeric class. Also supports arrays with depths higher than {@code 1}.
	 */
	public static Object convertNumericArray(Object array, Class<?> to) {
		Class<?> componentType = array.getClass().getComponentType();
		int length = Array.getLength(array);

		Object newArray;

		if (componentType.isArray()) {
			newArray = Array.newInstance(componentType, length);

			for (int i = 0; i < length; i++) {
				Object innerArray = Array.get(array, i);

				if (innerArray == null) {
					innerArray = Array.newInstance(componentType.getComponentType(), 0);
					Array.set(newArray, i, innerArray);
				} else {
					Array.set(newArray, i, convertNumericArray(innerArray, to));
				}
			}
		} else {
			newArray = Array.newInstance(to, length);

			for (int i = 0; i < length; i++) {
				Object what = Array.get(array, i);

				if (to == byte.class || to == Byte.class) {
					what = ((Number) what).byteValue();
				} else if (to == double.class || to == Double.class) {
					what = ((Number) what).doubleValue();
				} else if (to == float.class || to == Float.class) {
					what = ((Number) what).floatValue();
				} else if (to == int.class || to == Integer.class) {
					what = ((Number) what).intValue();
				} else if (to == long.class || to == Long.class) {
					what = ((Number) what).longValue();
				} else if (to == short.class || to == Short.class) {
					what = ((Number) what).shortValue();
				}

				Array.set(newArray, i , what);
			}
		}

		return newArray;
	}

	/**
	 * Gets the array depth of a given class, for example:<br>
	 *   Object[] -> 1<br>
	 *   Object[][][] -> 3
	 */
	public static int getArrayDepth(Class<?> cls) {
		int depth = 0;

		while (cls.isArray()) {
			cls = cls.getComponentType();
			depth++;
		}

		return depth;
	}

	/**
	 * @return the component type of the given array class, independent of the depth of the array.
	 */
	public static Class<?> getBaseComponent(Class<?> obj) {
		Class<?> componentType = obj.getComponentType();

		while (componentType.isArray()) {
			componentType = componentType.getComponentType();
		}

		return componentType;
	}

	/**
	 * An functional interface with an input and output that may throw an {@link Exception}.
	 * @param <T> the input of the function.
	 * @param <R> the return value of the function.
	 */
	@FunctionalInterface
	public interface ExceptionalFunction<T, R> {
		R apply(T t) throws Exception;
	}

	/**
	 * Converts a {@link ExceptionalFunction} to a normal {@link Function}, by catching the exception
	 * and logging a warning with the simple class name and message of the exception.
	 * If Skript's verbosity is set to 'very high' or above, this will also print the stack trace.
	 */
	public static <T, R> Function<T, R> propagateErrors(ExceptionalFunction<T, R> function) {
		return t -> {
			try {
				return function.apply(t);
			} catch (Exception e) {
				Skript.warning("skript-reflect encountered a " + e.getClass().getSimpleName() + ": " + e.getMessage());

				if (Skript.logVeryHigh()) {
					StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					Skript.warning(errors.toString());
				} else {
					Skript.warning("Run Skript with the verbosity 'very high' for the stack trace.");
				}

			}
			return null;
		};
	}

	/**
	 * @return whether the given class is a numeric class, albeit primitive or a wrapper of a primitive numeric class.
	 */
	public static boolean isNumericClass(Class<?> cls) {
		return Number.class.isAssignableFrom(cls) || JavaUtil.NUMERIC_CLASSES.contains(cls);
	}

	/**
	 * {@return} a new array with the given class as base component type, and the given length as the array length.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(Class<? extends T> type, int length) {
		return (T[]) Array.newInstance(type, length);
	}

	/**
	 * {@return} the array class of the given class.
	 */
	public static <T> Class<?> getArrayClass(Class<T> type) {
		return Array.newInstance(type, 0).getClass();
	}

	/**
	 * @return the array class with the given depth of the given type, using {@link #getArrayClass(Class)}.
	 */
	public static Class<?> getArrayClass(Class<?> type, int layers) {
		for (int i = 0; i < layers; i++) {
			type = getArrayClass(type);
		}

		return type;
	}

	/**
	 * @return the string form of a (possibly primitive) array, in the form [a, b, c, ...],
	 * converting the values to strings using the given {@link Function}.
	 */
	public static String arrayToString(Object array, Function<Object, String> function) {
		int length = Array.getLength(array);

		StringBuilder stringBuilder = new StringBuilder("[");
		for (int i = 0; i < length; i++) {
			Object value = Array.get(array, i);
			stringBuilder.append(function.apply(value));

			if (i != length - 1) {
				stringBuilder.append(", ");
			}
		}
		return stringBuilder.append("]").toString();
	}

	/**
	 * Returns whether the given {@link Object} can be converted to the given class.
	 */
	public static boolean canConvert(Object object, Class<?> to) {
		if (to.isInstance(object)) {
			return true;
		}

		// coerce numeric types
		if (object instanceof Number && JavaUtil.NUMERIC_CLASSES.contains(to)) {
			return true;
		}

		// coerce arrays of numeric types
		if (to.isArray() && JavaUtil.getArrayDepth(to) == JavaUtil.getArrayDepth(object.getClass())) {
			Class<?> paramComponent = JavaUtil.getBaseComponent(to);
			Class<?> argComponent = JavaUtil.getBaseComponent(object.getClass());

			if (JavaUtil.isNumericClass(paramComponent) && JavaUtil.isNumericClass(argComponent)) {
				return true;
			}
		}

		// allow boxed numbers
		if (to.isPrimitive() && JavaUtil.WRAPPER_CLASSES.get(to).isInstance(object)) {
			return true;
		}

		// coerce single character strings to chars
		if (object instanceof String
			&& (to == char.class || to == Character.class)
			&& ((String) object).length() == 1) {
			return true;
		}

		// coerce a Skript ItemType to an ItemStack
		if (object instanceof ItemType && to == ItemStack.class) {
			return true;
		}

		// coerce javaclasses and classinfos into classes
		if (to == Class.class && (object instanceof JavaType || object instanceof ClassInfo)) {
			return true;
		}

		// unwrap null wrapper
		return !to.isPrimitive() && object instanceof Null;
	}

	/**
	 * Converts the given {@link Object} to the given class.
	 * If {@link #canConvert(Object, Class)} returned {@code false}, this
	 * returns the same {@link Object} as was passed to this method.
	 */
	public static Object convert(Object object, Class<?> to) {
		// coerce numeric types
		if (object instanceof Number && JavaUtil.NUMERIC_CLASSES.contains(to)) {
			if (to == byte.class || to == Byte.class) {
				return ((Number) object).byteValue();
			} else if (to == double.class || to == Double.class) {
				return ((Number) object).doubleValue();
			} else if (to == float.class || to == Float.class) {
				return ((Number) object).floatValue();
			} else if (to == int.class || to == Integer.class) {
				return ((Number) object).intValue();
			} else if (to == long.class || to == Long.class) {
				return ((Number) object).longValue();
			} else if (to == short.class || to == Short.class) {
				return ((Number) object).shortValue();
			}
		}

		// coerce arrays of numeric types
		if (to.isArray()
			&& JavaUtil.getArrayDepth(to) == JavaUtil.getArrayDepth(object.getClass())
			&& JavaUtil.isNumericClass(JavaUtil.getBaseComponent(to))) {
			return JavaUtil.convertNumericArray(object, JavaUtil.getBaseComponent(to));
		}

		// coerce single character strings to chars
		if (object instanceof String && (to == char.class || to == Character.class)) {
			return ((String) object).charAt(0);
		}

		// coerce a Skript ItemType to an ItemStack
		if (object instanceof ItemType && to == ItemStack.class) {
			return ((ItemType) object).getRandom();
		}

		// coerce javatypes and classinfos into classes
		if (to == Class.class) {
			if (object instanceof JavaType) {
				return ((JavaType) object).getJavaClass();
			} else if (object instanceof ClassInfo) {
				return ((ClassInfo<?>) object).getC();
			}
		}

		// unwrap null wrapper
		if (object instanceof Null) {
			return null;
		}

		return object;
	}

}
