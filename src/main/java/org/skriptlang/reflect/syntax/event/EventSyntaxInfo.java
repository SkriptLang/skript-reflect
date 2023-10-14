package org.skriptlang.reflect.syntax.event;

import ch.njol.skript.lang.SkriptEventInfo;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import org.skriptlang.skript.lang.script.Script;

import java.util.Objects;

public class EventSyntaxInfo extends CustomSyntaxStructure.SyntaxData {

  protected EventSyntaxInfo(Script script, String pattern, int matchedPattern) {
    super(script, pattern, matchedPattern);
  }

  public static EventSyntaxInfo create(Script script, String pattern, int matchedPattern) {
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
