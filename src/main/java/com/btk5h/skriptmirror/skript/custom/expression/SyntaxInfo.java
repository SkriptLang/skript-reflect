package com.btk5h.skriptmirror.skript.custom.expression;

import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class SyntaxInfo implements CustomSyntaxSection.SyntaxData {
  private final String pattern;
  private final int[] inheritedSingles;
  private final boolean alwaysPlural;
  private final boolean adaptArgument;
  private final boolean property;


  private SyntaxInfo(String pattern, int[] inheritedSingles, boolean alwaysPlural,
             boolean adaptArgument, boolean property) {
    this.pattern = pattern;
    this.inheritedSingles = inheritedSingles;
    this.alwaysPlural = alwaysPlural;
    this.adaptArgument = adaptArgument;
    this.property = property;
  }

  public static SyntaxInfo create(String pattern, boolean alwaysPlural,
                                  boolean adaptArgument, boolean property) {
    StringBuilder newPattern = new StringBuilder(pattern.length());
    List<Integer> inheritedSingles = new ArrayList<>();
    String[] parts = pattern.split("%");

    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (i % 2 == 0) {
        newPattern.append(part);
      } else {
        if (part.startsWith("$")) {
          part = part.substring(1);
          inheritedSingles.add(i / 2);
        }

        if (part.startsWith("_")) {
          part = part.endsWith("s") ? "objects" : "object";
        }

        newPattern.append('%');
        newPattern.append(part);
        newPattern.append('%');
      }
    }

    return new SyntaxInfo(
        newPattern.toString(),
        inheritedSingles.stream()
            .mapToInt(i -> i)
            .toArray(),
        alwaysPlural,
        adaptArgument,
        property);
  }

  @Override
  public String getPattern() {
    return pattern;
  }

  public int[] getInheritedSingles() {
    return inheritedSingles;
  }

  public boolean isAlwaysPlural() {
    return alwaysPlural;
  }

  public boolean shouldAdaptArgument() {
    return adaptArgument;
  }

  public boolean isProperty() {
    return property;
  }

  @Override
  public String toString() {
    return String.format("%s (singles: %s, plural: %s, adapt: %s, property: %s)",
        pattern, Arrays.toString(inheritedSingles), alwaysPlural, adaptArgument, property);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SyntaxInfo that = (SyntaxInfo) o;
    return alwaysPlural == that.alwaysPlural &&
        adaptArgument == that.adaptArgument &&
        Objects.equals(pattern, that.pattern) &&
        Arrays.equals(inheritedSingles, that.inheritedSingles);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(pattern, alwaysPlural, adaptArgument);
    result = 31 * result + Arrays.hashCode(inheritedSingles);
    return result;
  }
}
