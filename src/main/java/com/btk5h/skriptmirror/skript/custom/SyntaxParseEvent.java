package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.util.SimpleLiteral;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.HandlerList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SyntaxParseEvent extends CustomSyntaxEvent {
  private final static HandlerList handlers = new HandlerList();
  private final Class<?>[] eventClasses;
  private boolean markedContinue;

  public SyntaxParseEvent(Expression<?>[] expressions, SkriptParser.ParseResult parseResult, Class<?>[] eventClasses) {
    super(null, wrapRawExpressions(expressions), parseResult);
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

  public void markContinue() {
    markedContinue = true;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  @SuppressWarnings("unchecked")
  public static <T extends CustomSyntaxSection.SyntaxData> void register(CustomSyntaxSection<T> section,
                                                                         SectionNode node,
                                                                         List<T> whichInfo, Map<T, Trigger> parserHandlers) {
    ScriptLoader.setCurrentEvent("custom syntax parser", SyntaxParseEvent.class);
    Util.getItemsFromNode(node, "parse").ifPresent(items ->
        whichInfo.forEach(which ->
            parserHandlers.put(which,
                new Trigger(ScriptLoader.currentScript.getFile(), "parse " + which.getPattern(), section, items))
        )
    );
  }
}
