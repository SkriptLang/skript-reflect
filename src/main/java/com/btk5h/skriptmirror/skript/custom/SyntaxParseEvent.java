package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.lang.util.SimpleLiteral;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.HandlerList;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SyntaxParseEvent extends CustomSyntaxEvent implements Continuable {

	private final static HandlerList handlers = new HandlerList();
	private final Class<?>[] eventClasses;
	private boolean markedContinue = false;

	public SyntaxParseEvent(Expression<?>[] expressions, int matchedPattern, SkriptParser.ParseResult parseResult,
							Class<?>[] eventClasses) {
		super(null, wrapRawExpressions(expressions), matchedPattern, parseResult);
		this.eventClasses = eventClasses;
	}

	private static Expression<?>[] wrapRawExpressions(Expression<?>[] expressions) {
		return Arrays.stream(expressions)
			.map(expr -> expr == null ? null : new SimpleLiteral<>(expr, false))
			.toArray(Expression[]::new);
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Class<?>[] getEventClasses() {
		return eventClasses;
	}

	public boolean isMarkedContinue() {
		return markedContinue;
	}

	@Override
	public void setContinue(boolean b) {
		markedContinue = b;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static <T extends CustomSyntaxStructure.SyntaxData> void register(SectionNode parseNode,
																			 List<T> whichInfo, Map<T, Trigger> parserHandlers) {
		ParserInstance.get().setCurrentEvent("custom syntax parser", SyntaxParseEvent.class);
		List<TriggerItem> items = SkriptUtil.getItemsFromNode(parseNode);

		whichInfo.forEach(which ->
			parserHandlers.put(which,
				new Trigger(ParserInstance.get().getCurrentScript(), "parse " + which.getPattern(), new SimpleEvent(), items)));
	}

}
