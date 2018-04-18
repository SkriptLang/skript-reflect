package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ExpressionGetEvent extends CustomSyntaxEvent {
  private final static HandlerList handlers = new HandlerList();
  private Object[] output;

  public ExpressionGetEvent(Event event, Expression<?>[] expressions,
                            SkriptParser.ParseResult parseResult) {
    super(event, expressions, parseResult);
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public Object[] getOutput() {
    return output;
  }

  public void setOutput(Object[] output) {
    this.output = output;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
