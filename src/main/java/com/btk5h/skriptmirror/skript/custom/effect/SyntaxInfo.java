package com.btk5h.skriptmirror.skript.custom.effect;

import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;

import java.io.File;
import java.util.Objects;

class SyntaxInfo extends CustomSyntaxSection.SyntaxData {
  private SyntaxInfo(File script, String pattern) {
    super(script, pattern);
  }

  public static SyntaxInfo create(File script, String pattern) {
    return new SyntaxInfo(script, SkriptMirrorUtil.preprocessPattern(pattern));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SyntaxInfo that = (SyntaxInfo) o;
    return Objects.equals(getScript(), that.getScript()) &&
        Objects.equals(getPattern(), that.getPattern());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getScript(), getPattern());
  }
}
