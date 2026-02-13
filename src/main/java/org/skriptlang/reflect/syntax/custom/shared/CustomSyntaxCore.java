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

	private final CustomSyntaxInfo<?> info;

	private Expression<?>[] expressions;
	private int matchedPattern;
	private ParseResult parseResult;
	private String usedPattern;
	private @Nullable SyntaxParseEvent parseEvent;

	public CustomSyntaxCore(CustomSyntaxInfo<?> info) {
		this.info = info;
	}

	public boolean preInit() {
		return info.canBeUsedIn(ParserInstance.get());
	}

	public boolean init(SyntaxElement self, Expression<?>[] expressions, int matchedPattern, ParseResult parseResult) {
		usedPattern = info.patterns()[matchedPattern];

		for (int i = 0; i < expressions.length; i++) {
			expressions[i] = LiteralUtils.defendExpression(expressions[i]);
		}
		if (!LiteralUtils.canInitSafely(Arrays.stream(expressions).filter(Objects::nonNull).toArray(Expression[]::new)))
			return false;

		this.expressions = expressions;
		this.matchedPattern = matchedPattern;
		this.parseResult = parseResult;

		if (!info.hasParseSection())
			return true;

		if (info.parseTrigger() == null) {
			Skript.error("Custom syntaxes with a 'parse' section cannot be used before they're loaded.");
			return false;
		}

		Class<? extends Event>[] events = ParserInstance.get().getCurrentEvents();
		parseEvent = new SyntaxParseEvent(self, expressions, matchedPattern, parseResult, events);
		TriggerItem.walk(info.parseTrigger(), parseEvent);
		return parseEvent.isMarkedContinue();
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

}
