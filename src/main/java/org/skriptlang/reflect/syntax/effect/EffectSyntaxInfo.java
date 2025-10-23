package org.skriptlang.reflect.syntax.effect;

import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import org.skriptlang.skript.lang.script.Script;

import java.util.Objects;

public class EffectSyntaxInfo extends CustomSyntaxStructure.SyntaxData {

	private EffectSyntaxInfo(Script script, String pattern, int matchedPattern) {
		super(script, pattern, matchedPattern);
	}

	public static EffectSyntaxInfo create(Script script, String pattern, int matchedPattern) {
		return new EffectSyntaxInfo(script, SkriptMirrorUtil.preprocessPattern(pattern), matchedPattern);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EffectSyntaxInfo that = (EffectSyntaxInfo) o;
		return Objects.equals(getScript(), that.getScript()) &&
			Objects.equals(getPattern(), that.getPattern());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getScript(), getPattern());
	}

}
