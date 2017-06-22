package com.btk5h.skriptmirror;

import java.util.function.Function;

import ch.njol.skript.Skript;

public final class Util {
  private Util () {}

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
}
