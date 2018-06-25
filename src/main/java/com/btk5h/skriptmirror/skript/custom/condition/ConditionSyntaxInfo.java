package com.btk5h.skriptmirror.skript.custom.condition;

import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;

import java.io.File;
import java.util.Objects;

public class ConditionSyntaxInfo extends CustomSyntaxSection.SyntaxData {
  private final boolean inverted;
  private final boolean property;

  private ConditionSyntaxInfo(File script, String pattern, int matchedPattern, boolean inverted, boolean property) {
    super(script, pattern, matchedPattern);
    this.inverted = inverted;
    this.property = property;
  }

  public static ConditionSyntaxInfo create(File script, String pattern, int matchedPattern,
                                           boolean inverted, boolean property) {
    return new ConditionSyntaxInfo(script, SkriptMirrorUtil.preprocessPattern(pattern), matchedPattern,
        inverted, property);
  }

  public boolean isInverted() {
    return inverted;
  }

  public boolean isProperty() {
    return property;
  }

  @Override
  public String toString() {
    return String.format("%s (inverted: %s, property: %s)", getPattern(), inverted, property);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConditionSyntaxInfo that = (ConditionSyntaxInfo) o;
    return inverted == that.inverted &&
        property == that.property &&
        Objects.equals(getScript(), that.getScript()) &&
        Objects.equals(getPattern(), that.getPattern());
  }

  @Override
  public int hashCode() {
    return Objects.hash(inverted, property, getScript(), getPattern());
  }
}
