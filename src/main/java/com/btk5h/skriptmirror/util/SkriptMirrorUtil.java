package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SkriptMirrorUtil {
  public static final String IDENTIFIER = "[_a-zA-Z$][\\w$]*";
  public static final String PACKAGE = "(?:" + IDENTIFIER + "\\.)*(?:" + IDENTIFIER + ")";

  private static final Pattern TYPE_PREFIXES = Pattern.compile("^[-*~]*");

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
          part = processTypes(part);
        }

        newPattern.append('%');
        newPattern.append(part);
        newPattern.append('%');
      }
    }

    return newPattern.toString();
  }

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
        suffixes = part.substring(timeIndex, part.length());
        part = part.substring(0, timeIndex);
      }

      // replace user input patterns
      String types = Arrays.stream(part.split("/"))
          .map(SkriptMirrorUtil::replaceUserInputPatterns)
          .collect(Collectors.joining("/"));

      return prefixes + types + suffixes;
    }
    return part;
  }

  private static String replaceUserInputPatterns(String part) {
    NonNullPair<String, Boolean> info = Utils.getEnglishPlural(part);

    ClassInfo<?> ci = Classes.getClassInfoFromUserInput(info.getFirst());

    if (ci == null) {
      Skript.warning(String.format("'%s' is not a valid Skript type. Using 'object' instead.", part));
      return info.getSecond() ? "objects" : "object";
    }

    return Utils.toEnglishPlural(ci.getCodeName(), info.getSecond());
  }
}
