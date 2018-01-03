package com.btk5h.skriptmirror;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.SyntaxElementInfo;

public final class Util {
  public static final Map<Class<?>, Class<?>> WRAPPER_CLASSES = new HashMap<>();
  public static final Set<Class<?>> NUMERIC_CLASSES = new HashSet<>();

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
  }

  private Util () {}

  private static Field PATTERNS;

  static {
    Field _PATTERNS = null;
    try {
      _PATTERNS = SyntaxElementInfo.class.getDeclaredField("patterns");
      _PATTERNS.setAccessible(true);
    } catch (NoSuchFieldException e) {
      Skript.warning("Skript's pattern info field could not be resolved. " +
          "Custom syntax will not work.");
    }
    PATTERNS = _PATTERNS;
  }

  public static void setPatterns(SyntaxElementInfo<?> info, String[] patterns) {
    if (PATTERNS != null) {
      try {
        PATTERNS.set(info, patterns);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  public static Stream<Field> fields(Class<?> cls) {
    return Stream.concat(
        Arrays.stream(cls.getFields()),
        Arrays.stream(cls.getDeclaredFields())
            .filter(Util::notPublic)
    );
  }

  public static Stream<Method> methods(Class<?> cls) {
    return Stream.concat(
        Arrays.stream(cls.getMethods()),
        Arrays.stream(cls.getDeclaredMethods())
            .filter(Util::notPublic)
    );
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

  public static boolean notPublic(Member m) {
    return !Modifier.isPublic(m.getModifiers());
  }

  public static String preprocessPattern(String pattern) {
    StringBuilder newPattern = new StringBuilder(pattern.length());
    String[] parts = pattern.split("%");

    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (i % 2 == 0) {
        newPattern.append(part);
      } else {
        if (part.startsWith("_")) {
          part = part.endsWith("s") ? "objects" : "object";
        }

        newPattern.append('%');
        newPattern.append(part);
        newPattern.append('%');
      }
    }

    return newPattern.toString();
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

  public static Class<?> toClass(Object o) {
    if (o instanceof JavaType) {
      return ((JavaType) o).getJavaClass();
    }
    return o.getClass();
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] newArray(Class<? extends T> type, int length) {
    return (T[]) Array.newInstance(type, length);
  }

  public static String getDebugName(Class<?> cls) {
    return Skript.logVeryHigh() ? cls.getName() : cls.getSimpleName();
  }
}
