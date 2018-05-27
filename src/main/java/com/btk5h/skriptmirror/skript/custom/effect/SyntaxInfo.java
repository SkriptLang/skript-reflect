package com.btk5h.skriptmirror.skript.custom.effect;

import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;

import java.io.File;
import java.util.Objects;

class SyntaxInfo implements CustomSyntaxSection.SyntaxData {
  private final File script;
  private final String pattern;

  private SyntaxInfo(File script, String pattern) {
    this.script = script;
    this.pattern = pattern;
  }

  public static SyntaxInfo create(File script, String pattern) {
    return new SyntaxInfo(script, SkriptMirrorUtil.preprocessPattern(pattern));
  }

  @Override
  public File getScript() {
    return script;
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
