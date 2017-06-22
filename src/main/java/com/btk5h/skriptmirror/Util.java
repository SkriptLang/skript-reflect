package com.btk5h.skriptmirror;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.njol.skript.Skript;

public final class Util {
  private Util () {}

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
        Skript.exception(e);
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
}
