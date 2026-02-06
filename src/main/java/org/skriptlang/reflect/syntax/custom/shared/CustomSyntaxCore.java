package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.util.LiteralUtils;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.util.Priority;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

public class CustomSyntaxCore {

	private final String[] patterns;
	private final boolean hasParseSection;
	private final @Nullable Script script;
	private final Predicate<ParserInstance> usableInPredicate;

	private @Nullable Trigger parseTrigger;

	private Expression<?>[] expressions;
	private int matchedPattern;
	private ParseResult parseResult;
	private String usedPattern;
	private @Nullable SyntaxParseEvent parseEvent;

	public CustomSyntaxCore(
		String[] patterns,
		boolean hasParseSection,
		@Nullable Script script,
		Predicate<ParserInstance> usableInPredicate
	) {
		this(patterns, hasParseSection, script, usableInPredicate, null);
	}

	public CustomSyntaxCore(
		String[] patterns,
		boolean hasParseSection,
		@Nullable Script script,
		Predicate<ParserInstance> usableInPredicate,
		@Nullable Trigger parseTrigger
	) {
		for (int i = 0; i < patterns.length; i++)
			patterns[i] = SkriptMirrorUtil.preprocessPattern(patterns[i]);
		this.patterns = patterns;
		this.hasParseSection = hasParseSection;
		this.script = script;
		this.usableInPredicate = usableInPredicate;
		this.parseTrigger = parseTrigger;
	}

	public boolean preInit() {
		ParserInstance parser = ParserInstance.get();
		if (script != null && (!parser.isActive() || parser.getCurrentScript() != script))
			return false;

		return usableInPredicate == null || usableInPredicate.test(parser);
	}

	public boolean init(SyntaxElement self, Expression<?>[] expressions, int matchedPattern, ParseResult parseResult) {
		usedPattern = patterns[matchedPattern];

		for (int i = 0; i < expressions.length; i++) {
			expressions[i] = LiteralUtils.defendExpression(expressions[i]);
		}
		if (!LiteralUtils.canInitSafely(Arrays.stream(expressions).filter(Objects::nonNull).toArray(Expression[]::new)))
			return false;

		this.expressions = expressions;
		this.matchedPattern = matchedPattern;
		this.parseResult = parseResult;

		if (!hasParseSection)
			return true;

		if (parseTrigger == null) {
			Skript.error("Custom syntaxes with a 'parse' section cannot be used before they're loaded.");
			return false;
		}

		Class<? extends Event>[] events = ParserInstance.get().getCurrentEvents();
		parseEvent = new SyntaxParseEvent(self, expressions, matchedPattern, parseResult, events);
		TriggerItem.walk(parseTrigger, parseEvent);
		return parseEvent.isMarkedContinue();
	}

	public String[] patterns() {
		return patterns;
	}

	public Script script() {
		return script;
	}

	public boolean local() {
		return script() != null;
	}

	public Trigger parseTrigger() {
		return parseTrigger;
	}

	public void parseTrigger(Trigger parseTrigger) {
		if (this.parseTrigger != null)
			throw new IllegalStateException("Parse trigger is already set!");
		this.parseTrigger = parseTrigger;
	}

	public Expression<?>[] expressions() {
		return expressions;
	}

	public int matchedPattern() {
		return matchedPattern;
	}

	public ParseResult parseResult() {
		return parseResult;
	}

	public String usedPattern() {
		return usedPattern;
	}

	public @Nullable SyntaxParseEvent parseEvent() {
		return parseEvent;
	}

	public Priority priority() {
		return local() ? CustomSyntax.LOCAL_PRIORITY : CustomSyntax.PRIORITY;
	}

	public CustomSyntaxCore copy() {
		return new CustomSyntaxCore(patterns, hasParseSection, script, usableInPredicate, parseTrigger);
	}

}
