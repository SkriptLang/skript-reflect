package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;

public class SkriptMirrorUtil {
  public static final String IDENTIFIER = "[_a-zA-Z$][\\w$]*";
  public static final String PACKAGE = "(?:" + IDENTIFIER + "\\.)*(?:" + IDENTIFIER + ")";

  public static Class<?> toClassUnwrapJavaTypes(Object o) {
    if (o instanceof JavaType) {
      return ((JavaType) o).getJavaClass();
    }

    return getClass(o);
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
        } else {
          part = replaceUserInputPatterns(part);
        }

        newPattern.append('%');
        newPattern.append(part);
        newPattern.append('%');
      }
    }

    return newPattern.toString();
  }

  public static String replaceUserInputPatterns(String part) {
    if (part.length() > 0) {
      ClassInfo<?> ci;
      ci = Classes.getClassInfoNoError(part);

      if (ci == null) {
        ci = Classes.getClassInfoFromUserInput(part);
      }

      if (ci == null) {
        Skript.warning(String.format("'%s' is not a valid Skript type. Using 'object' instead.", part));
        part = part.endsWith("s") ? "objects" : "object";
      } else {
        part = part.endsWith("s") ? ci.getCodeName() + "s" : ci.getCodeName();
      }
    }
    return part;
  }
}
