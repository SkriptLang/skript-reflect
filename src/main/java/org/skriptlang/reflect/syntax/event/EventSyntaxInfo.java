package org.skriptlang.reflect.syntax.event;

import ch.njol.skript.Skript;
import ch.njol.skript.util.Version;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.skript.lang.script.Script;

import java.util.Objects;

public class EventSyntaxInfo extends CustomSyntaxStructure.SyntaxData {

	protected EventSyntaxInfo(Script script, String pattern, int matchedPattern) {
		super(script, pattern, matchedPattern);
	}

	public static EventSyntaxInfo create(Script script, String pattern, int matchedPattern) {
		if (Skript.getVersion().isSmallerThan(new Version(2, 8)))
			pattern = "[on] " + pattern + " [with priority (lowest|low|normal|high|highest|monitor)]";
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
