package com.btk5h.skriptmirror.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class JavaReflection {
  private static Field MODIFIERS;

  static {
    Field _FIELD;

    try {
      _FIELD = Field.class.getDeclaredField("modifiers");
      _FIELD.setAccessible(true);
      MODIFIERS = _FIELD;
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static void removeFinalModifier(Field field) throws IllegalAccessException {
    MODIFIERS.set(field, field.getModifiers() & ~Modifier.FINAL);
  }
}
