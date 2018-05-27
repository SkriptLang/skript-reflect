package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.util.*;
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

    PRIMITIVE_CLASS_NAMES.put("boolean", boolean.class);
    PRIMITIVE_CLASS_NAMES.put("byte", byte.class);
    PRIMITIVE_CLASS_NAMES.put("char", char.class);
    PRIMITIVE_CLASS_NAMES.put("double", double.class);
    PRIMITIVE_CLASS_NAMES.put("float", float.class);
    PRIMITIVE_CLASS_NAMES.put("int", int.class);
    PRIMITIVE_CLASS_NAMES.put("long", long.class);
    PRIMITIVE_CLASS_NAMES.put("short", short.class);
  }

  public static Stream<Field> fields(Class<?> cls) {
    return Stream.concat(
        Arrays.stream(cls.getFields()),
        Arrays.stream(cls.getDeclaredFields())
    ).distinct();
  }

  public static Stream<Method> methods(Class<?> cls) {
    return Stream.concat(
        Arrays.stream(cls.getMethods()),
        Arrays.stream(cls.getDeclaredMethods())
    ).distinct();
  }

  public static Stream<Constructor> constructor(Class<?> cls) {
    return Arrays.stream(cls.getDeclaredConstructors());
  }

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

  public static Object boxPrimitiveArray(Object obj) {
    Class<?> componentType = obj.getClass().getComponentType();
    if (componentType != null && componentType.isPrimitive()) {
      int length = Array.getLength(obj);
      Object[] boxedArray = newArray(WRAPPER_CLASSES.get(componentType), length);

      for (int i = 0; i < length; i++) {
        boxedArray[i] = Array.get(obj, i);
      }

      obj = boxedArray;
    }
    return obj;
  }

  @FunctionalInterface
  public interface ExceptionalFunction<T, R> {
    R apply(T t) throws Exception;
  }

  @SuppressWarnings("ThrowableNotThrown")
  public static <T, R> Function<T, R> propagateErrors(ExceptionalFunction<T, R> function) {
    return t -> {
      try {
        return function.apply(t);
      } catch (Exception e) {
        Skript.warning(
            String.format("skript-mirror encountered a %s: %s%n" +
                    "Run Skript with the verbosity 'very high' for the stack trace.",
                e.getClass().getSimpleName(), e.getMessage()));

        if (Skript.logVeryHigh()) {
          StringWriter errors = new StringWriter();
          e.printStackTrace(new PrintWriter(errors));
          Skript.warning(errors.toString());
        }
      }
      return null;
    };
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] newArray(Class<? extends T> type, int length) {
    return (T[]) Array.newInstance(type, length);
  }

}
