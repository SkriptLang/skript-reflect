package com.btk5h.skriptmirror;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Descriptor {
  private static final Pattern IDENTIFIER = Pattern.compile(
      "(?:\\[((?:[_a-zA-Z$][\\w$]*\\.)*(?:[_a-zA-Z$][\\w$]*))\\])?([_a-zA-Z$][\\w$]*)");

  private final Class<?> javaClass;
  private final String identifier;

  private Descriptor(Class<?> javaClass, String identifier) {
    this.javaClass = javaClass;
    this.identifier = identifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Descriptor that = (Descriptor) o;
    return Objects.equals(javaClass, that.javaClass) &&
        Objects.equals(identifier, that.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(javaClass, identifier);
  }

  public Class<?> getJavaClass() {
    return javaClass;
  }

  public String getIdentifier() {
    return identifier;
  }

  @Override
  public String toString() {
    return String.format("%s#%s",
        javaClass == null ? "(unspecified)" : Util.getDebugName(javaClass),
        identifier);
  }

  public static Descriptor parse(String desc) throws ClassNotFoundException {
    Matcher m = IDENTIFIER.matcher(desc);

    if (m.matches()) {
      String cls = m.group(1);
      String identifier = m.group(2);

      Class<?> javaClass = null;

      if (cls != null) {
        javaClass = Class.forName(cls);
      }

      return new Descriptor(javaClass, identifier);
    }

    return null;
  }

  public static Descriptor create(Class<?> javaClass, String identifier) {
    return new Descriptor(javaClass, identifier);
  }

  public static Descriptor create(String identifier) {
    return new Descriptor(null, identifier);
  }
}
