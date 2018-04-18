package com.btk5h.skriptmirror;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Descriptor {
  private static final String IDENTIFIER = "[_a-zA-Z$][\\w$]*";
  private static final String PACKAGE = "(?:" + IDENTIFIER + "\\.)*(?:" + IDENTIFIER + ")";
  private static final Pattern DESCRIPTOR =
      Pattern.compile("" +
          "(?:\\[(" + PACKAGE + ")])?" +
          "(" + IDENTIFIER + ")" +
          "(?:\\[((?:" + PACKAGE + "\\s*,\\s*)*(?:" + PACKAGE + "))])?"
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
    return String.format("%s#%s",
        javaClass == null ? "(unspecified)" : Util.getDebugName(javaClass),
        name);
  }

  public static Descriptor parse(String desc) throws ClassNotFoundException {
    Matcher m = DESCRIPTOR.matcher(desc);

    if (m.matches()) {
      String cls = m.group(1);
      String name = m.group(2);
      String args = m.group(3);

      Class<?> javaClass = null;
      Class<?>[] parameterTypes = null;

      if (cls != null) {
        javaClass = LibraryLoader.getClassLoader().loadClass(cls);
      }

      if (args != null) {
        parameterTypes = parseParams(args);
      }

      return new Descriptor(javaClass, name, parameterTypes);
    }

    return null;
  }

  private static Class<?>[] parseParams(String args) throws ClassNotFoundException {
    String[] rawClasses = args.split(",");
    Class<?>[] parsedClasses = new Class<?>[rawClasses.length];
    for (int i = 0; i < rawClasses.length; i++) {
      String userType = rawClasses[i];
      String normalType = userType.trim();
      Class<?> cls = Util.PRIMITIVE_CLASS_NAMES.get(normalType);

      if (cls == null) {
        cls = LibraryLoader.getClassLoader().loadClass(normalType);
      }

      parsedClasses[i] = cls;
    }
    return parsedClasses;
  }
}
