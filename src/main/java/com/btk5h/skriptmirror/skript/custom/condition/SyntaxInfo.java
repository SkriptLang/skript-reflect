package com.btk5h.skriptmirror.skript.custom.condition;

import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;

import java.io.File;
import java.util.Objects;

class SyntaxInfo extends CustomSyntaxSection.SyntaxData {
  private final boolean inverted;
  private final boolean property;

  private SyntaxInfo(File script, String pattern, boolean inverted, boolean property) {
    super(script, pattern);
    this.inverted = inverted;
    this.property = property;
  }

  public static SyntaxInfo create(File script, String pattern, boolean inverted, boolean property) {
    return new SyntaxInfo(script, SkriptMirrorUtil.preprocessPattern(pattern), inverted, property);
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
    SyntaxInfo that = (SyntaxInfo) o;
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
