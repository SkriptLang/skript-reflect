package com.btk5h.skriptmirror.skript.custom;

import com.btk5h.skriptmirror.WrappedEvent;

import org.bukkit.event.Event;

import java.util.Arrays;

public abstract class CustomSyntaxEvent extends WrappedEvent {
  private Expression<?>[] expressions;
  private final SkriptParser.ParseResult parseResult;

  protected CustomSyntaxEvent(Event event, Expression<?>[] expressions,
                              SkriptParser.ParseResult parseResult) {
    super(event);
    this.expressions = Arrays.stream(expressions)
        .map(expr -> CustomSyntaxExpression.wrap(expr, event))
        .toArray(Expression[]::new);
    this.parseResult = parseResult;
  }

  public Expression<?>[] getExpressions() {
    return expressions;
  }

  public SkriptParser.ParseResult getParseResult() {
    return parseResult;
  }
}
