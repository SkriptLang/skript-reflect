package org.skriptlang.reflect.syntax.condition;

import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import org.skriptlang.skript.lang.script.Script;

import java.util.Objects;

public class ConditionSyntaxInfo extends CustomSyntaxStructure.SyntaxData {

	private final boolean inverted;
	private final boolean property;

	private ConditionSyntaxInfo(Script script, String pattern, int matchedPattern, boolean inverted, boolean property) {
		super(script, pattern, matchedPattern);
		this.inverted = inverted;
		this.property = property;
	}

	public static ConditionSyntaxInfo create(Script script, String pattern, int matchedPattern,
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
