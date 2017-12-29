package com.btk5h.skriptmirror.skript.custom;

import com.btk5h.skriptmirror.WrappedEvent;

import org.bukkit.event.Event;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;

public abstract class CustomSyntaxEvent extends WrappedEvent {
  private Expression<?>[] expressions;
  private final SkriptParser.ParseResult parseResult;

  protected CustomSyntaxEvent(Event event, Expression<?>[] expressions,
                              SkriptParser.ParseResult parseResult) {
    super(event);
    this.expressions = expressions;
    this.parseResult = parseResult;
  }

  public Expression<?>[] getExpressions() {
    return expressions;
  }

  public SkriptParser.ParseResult getParseResult() {
    return parseResult;
  }
}
