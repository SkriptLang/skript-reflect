package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;

public class SkriptMirrorUtil {
  public static final String IDENTIFIER = "[_a-zA-Z$][\\w$]*";
  public static final String PACKAGE = "(?:" + IDENTIFIER + "\\.)*(?:" + IDENTIFIER + ")";

  public static Class<?> toClass(Object o) {
    if (o instanceof JavaType) {
      return ((JavaType) o).getJavaClass();
    }
    return o.getClass();
  }

  public static String getDebugName(Class<?> cls) {
    return Skript.logVeryHigh() ? cls.getName() : cls.getSimpleName();
  }

  public static Class<?> getClass(Object o) {
    if (o instanceof ObjectWrapper) {
      return ((ObjectWrapper) o).get().getClass();
    }

    if (o == null) {
      return Object.class;
    }

    return o.getClass();
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
          part = part.endsWith("s") ? "javaobjects" : "javaobject";
        }

        newPattern.append('%');
        newPattern.append(part);
        newPattern.append('%');
      }
    }

    return newPattern.toString();
  }
}
