package com.btk5h.skriptmirror.skript.custom.condition;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Name("Condition Check Event")
@Description({"You can find more information about this here: https://tpgamesnl.gitbook.io/skript-reflect/advanced/custom-syntax/conditions"})
public class ConditionCheckEvent extends CustomSyntaxEvent {
  private final static HandlerList handlers = new HandlerList();
  private boolean markedContinue;
  private boolean markedNegated;

  public ConditionCheckEvent(Event event, Expression<?>[] expressions, int matchedPattern,
                             SkriptParser.ParseResult parseResult) {
    super(event, expressions, matchedPattern, parseResult);
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public boolean isMarkedContinue() {
    return markedContinue;
  }

  public boolean isMarkedNegated() {
    return markedNegated;
  }

  public void markContinue() {
    markedContinue = true;
  }

  public void markNegated() {
    markedNegated = true;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
