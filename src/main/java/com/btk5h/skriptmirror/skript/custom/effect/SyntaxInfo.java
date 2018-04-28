package com.btk5h.skriptmirror.skript.custom.effect;

import com.btk5h.skriptmirror.Util;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;

import java.util.Objects;

class SyntaxInfo implements CustomSyntaxSection.SyntaxData {
  private final String pattern;

  private SyntaxInfo(String pattern) {
    this.pattern = pattern;
  }

  public static SyntaxInfo create(String pattern) {
    return new SyntaxInfo(Util.preprocessPattern(pattern));
  }

  @Override
  public String getPattern() {
    return pattern;
  }

  @Override
  public String toString() {
    return pattern;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SyntaxInfo that = (SyntaxInfo) o;
    return Objects.equals(pattern, that.pattern);
  }

  @Override
  public int hashCode() {

    return Objects.hash(pattern);
  }
}
