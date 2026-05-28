package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

import java.util.function.Predicate;

public abstract class CustomSyntaxInfo<T extends CustomSyntax> {

	protected final String[] patterns;
	protected final boolean hasParseSection;
	protected final @Nullable Script script;
	protected final Predicate<ParserInstance> usableInPredicate;

	protected @Nullable Trigger parseTrigger;

	public CustomSyntaxInfo(String[] patterns, boolean hasParseSection, @Nullable Script script, Predicate<ParserInstance> usableInPredicate) {
		for (int i = 0; i < patterns.length; i++)
			patterns[i] = SkriptMirrorUtil.preprocessPattern(patterns[i]);
		this.patterns = patterns;
		this.hasParseSection = hasParseSection;
		this.script = script;
		this.usableInPredicate = usableInPredicate;
	}

	public String[] patterns() {
		return patterns;
	}

	public boolean hasParseSection() {
		return hasParseSection;
	}

	public Script script() {
		return script;
	}

	public boolean local() {
		return script() != null;
	}

	public boolean canBeUsedIn(ParserInstance parser) {
		if (script != null && (!parser.isActive() || parser.getCurrentScript() != script))
			return false;
		return usableInPredicate == null || usableInPredicate.test(parser);
	}

	public Trigger parseTrigger() {
		return parseTrigger;
	}

	public void parseTrigger(Trigger parseTrigger) {
		if (this.parseTrigger != null)
			throw new IllegalStateException("Parse trigger is already set!");
		this.parseTrigger = parseTrigger;
	}

	public abstract boolean register(SyntaxRegistry registry);

	public abstract boolean unregister(SyntaxRegistry registry);

	public abstract T newInstance();

}
