package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.variables.Variables;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class CustomSyntaxCore {

	private final CustomSyntaxInfo<?> info;

	private Expression<?>[] expressions;
	private int matchedPattern;
	private ParseResult parseResult;
	private String usedPattern;
	private @Nullable Object localVariables;

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
		SyntaxParseEvent parseEvent = new SyntaxParseEvent(self, expressions, matchedPattern, parseResult, events);
		TriggerItem.walk(info.parseTrigger(), parseEvent);
		if (!parseEvent.isMarkedContinue())
			return false;
		localVariables = Variables.copyLocalVariables(parseEvent);
		return true;
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

	public @Nullable Object localVariables() {
		return localVariables;
	}

	public void walk(TriggerItem trigger, Event event) {
		if (localVariables == null) {
			TriggerItem.walk(trigger, event);
			return;
		}

		Variables.setLocalVariables(event, localVariables);
		TriggerItem.walk(trigger, event);
		Variables.removeLocals(event);
	}

}
