package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.*;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Arrays;

/**
 * This is the Skript event for all custom classes.
 * {@link #getLastWhich()} ()} returns the last EventSyntaxInfo of the last custom event, which is used internally for determining
 * which event-values can be used in parse-time.
 */
public class CustomEvent extends SkriptEvent {

//  private static CustomEvent lastCustomEvent;
  private static EventSyntaxInfo lastWhich;

  private EventSyntaxInfo which;
  private Expression<?>[] exprs;
  private SkriptParser.ParseResult parseResult;
  private Object variablesMap;

  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
    which = CustomEventSection.lookup(SkriptUtil.getCurrentScript(), matchedPattern);

    if (which == null) {
      return false;
    }

    this.exprs = Arrays.stream(args)
      .map(SkriptUtil::defendExpression)
      .toArray(Expression[]::new);
    this.parseResult = parseResult;

    if (!SkriptUtil.canInitSafely(this.exprs)) {
      return false;
    }

//    lastCustomEvent = this;
    lastWhich = which;

    Trigger parseHandler = CustomEventSection.parserHandlers.get(which);

    if (parseHandler == null)
      return true;

    SyntaxParseEvent event =
      new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, ScriptLoader.getCurrentEvents());

    // Because of link below, Trigger#execute removes local variables
    // https://github.com/SkriptLang/Skript/commit/a6661c863bae65e96113b69bebeaab51d814e2b9
    TriggerItem.walk(parseHandler, event);
    variablesMap = SkriptReflection.removeLocals(event);

    return event.isMarkedContinue();
  }

  @Override
  public boolean check(Event e) {
    BukkitCustomEvent bukkitCustomEvent = (BukkitCustomEvent) e;
    if (!bukkitCustomEvent.getName().equalsIgnoreCase(CustomEventSection.nameValues.get(which)))
      return false;

    EventTriggerEvent eventTriggerEvent = new EventTriggerEvent(e, exprs, which.getMatchedPattern(), parseResult, which.getPattern());
    SkriptReflection.copyVariablesMapFromMap(variablesMap, eventTriggerEvent);

    Trigger trigger = CustomEventSection.eventHandlers.get(which);
    if (trigger != null) {
      trigger.execute(eventTriggerEvent);
      return eventTriggerEvent.isMarkedContinue();
    }

    return true;
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return which.getPattern();
  }

  public static EventSyntaxInfo getLastWhich() {
    return lastWhich;
  }

  public static void setLastWhich(EventSyntaxInfo which) {
    lastWhich = which;
  }

  public EventSyntaxInfo getWhich() {
    return which;
  }

}
