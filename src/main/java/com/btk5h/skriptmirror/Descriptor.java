package com.btk5h.skriptmirror;

import com.btk5h.skriptmirror.skript.custom.CustomImport;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Descriptor {
  private static final String PACKAGE_ARRAY = SkriptMirrorUtil.PACKAGE + "(?:\\[])*";
  private static final Pattern PACKAGE_ARRAY_SINGLE = Pattern.compile("\\[]");
  private static final Pattern DESCRIPTOR =
      Pattern.compile("" +
          "(?:\\[(" + SkriptMirrorUtil.PACKAGE + ")])?" +
          "(" + SkriptMirrorUtil.IDENTIFIER + ")" +
          "(?:\\[((?:" + PACKAGE_ARRAY + "\\s*,\\s*)*(?:" + PACKAGE_ARRAY + "))])?"
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
    return toString(false);
  }

  public String toString(boolean isStatic) {
    return String.format("%s" + (isStatic ? "." : "#") + "%s",
      javaClass == null ? "(unspecified)" : SkriptMirrorUtil.getDebugName(javaClass),
      name);
  }

  public Descriptor orDefaultClass(Class<?> cls) {
    if (getJavaClass() != null) {
      return this;
    }

    return new Descriptor(cls, getName(), getParameterTypes());
  }

  public static Descriptor parse(String desc, File script) throws ClassNotFoundException {
    Matcher m = DESCRIPTOR.matcher(desc);

    if (m.matches()) {
      String cls = m.group(1);
      String name = m.group(2);
      String args = m.group(3);

      Class<?> javaClass = null;
      Class<?>[] parameterTypes = null;

      if (cls != null) {
        javaClass = lookupClass(script, cls);
      }

      if (args != null) {
        parameterTypes = parseParams(args, script);
      }

      return new Descriptor(javaClass, name, parameterTypes);
    }

    return null;
  }

  private static Class<?>[] parseParams(String args, File script) throws ClassNotFoundException {
    String[] rawClasses = args.split(",");
    Class<?>[] parsedClasses = new Class<?>[rawClasses.length];
    for (int i = 0; i < rawClasses.length; i++) {
      String userType = rawClasses[i].trim();

      Matcher arrayDepthMatcher = PACKAGE_ARRAY_SINGLE.matcher(userType);
      int arrayDepth = 0;
      while (arrayDepthMatcher.find()) {
        arrayDepth++;
      }
      userType = userType.substring(0, userType.length() - (2 * arrayDepth));

      Class<?> cls = JavaUtil.PRIMITIVE_CLASS_NAMES.get(userType);

      if (cls == null) {
        cls = lookupClass(script, userType);
      }

      cls = JavaUtil.getArrayClass(cls, arrayDepth);

      parsedClasses[i] = cls;
    }

    return parsedClasses;
  }

  private static Class<?> lookupClass(File script, String userType) throws ClassNotFoundException {
    JavaType customImport = CustomImport.lookup(script, userType);

    if (customImport != null) {
      return customImport.getJavaClass();
    } else {
      return LibraryLoader.getClassLoader().loadClass(userType);
    }
  }
}
