package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.lang.SkriptEventInfo;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;

import java.io.File;
import java.util.Objects;

public class EventSyntaxInfo extends CustomSyntaxSection.SyntaxData {

  protected EventSyntaxInfo(File script, String pattern, int matchedPattern) {
    super(script, pattern, matchedPattern);
  }

  public static EventSyntaxInfo create(File script, String pattern, int matchedPattern) {
    pattern = "[on] " + pattern + SkriptEventInfo.EVENT_PRIORITY_SYNTAX;

    return new EventSyntaxInfo(script, SkriptMirrorUtil.preprocessPattern(pattern), matchedPattern);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    EventSyntaxInfo that = (EventSyntaxInfo) o;
    return Objects.equals(getScript(), that.getScript()) &&
      Objects.equals(getPattern(), that.getPattern());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getScript(), getPattern());
  }

}
