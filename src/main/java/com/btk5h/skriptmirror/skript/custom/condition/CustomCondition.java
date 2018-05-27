package com.btk5h.skriptmirror.skript.custom.condition;


import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.util.Arrays;

public class CustomCondition extends Condition {
  private SyntaxInfo which;
  private Expression<?>[] exprs;
  private SkriptParser.ParseResult parseResult;
  private Event parseEvent;

  @Override
  public boolean check(Event e) {
    Trigger checker = CustomConditionSection.conditionHandlers.get(which);

    if (checker == null) {
      Skript.error(
          String.format("The custom condtion '%s' no longer has a check handler.", which.getPattern())
      );
      return false;
    }

    if (which.isProperty()) {
      return checkByProperty(e, checker);
    }

    return checkByStandard(e, checker);
  }

  private boolean checkByStandard(Event e, Trigger checker) {
    ConditionCheckEvent conditionEvent = new ConditionCheckEvent(e, exprs, parseResult);
    SkriptReflection.copyVariablesMap(parseEvent, conditionEvent);
    checker.execute(conditionEvent);
    return conditionEvent.isMarkedContinue() ^ conditionEvent.isMarkedNegated() ^ which.isInverted();
  }

  private boolean checkByProperty(Event e, Trigger checker) {
    return exprs[0].check(e, o -> {
      Expression<?>[] localExprs = Arrays.copyOf(exprs, exprs.length);
      localExprs[0] = new SimpleLiteral<>(o, false);

      ConditionCheckEvent conditionEvent = new ConditionCheckEvent(e, localExprs, parseResult);
      SkriptReflection.copyVariablesMap(parseEvent, conditionEvent);
      checker.execute(conditionEvent);
      return conditionEvent.isMarkedContinue() ^ conditionEvent.isMarkedNegated();
    }, which.isInverted());
  }

  @Override
  public String toString(Event e, boolean debug) {
    return which.getPattern();
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    which = CustomConditionSection.lookup(SkriptUtil.getCurrentScript(), matchedPattern);

    if (which == null) {
      return false;
    }

    this.exprs = Arrays.stream(exprs)
        .map(SkriptUtil::defendExpression)
        .toArray(Expression[]::new);
    this.parseResult = parseResult;

    if (!SkriptUtil.canInitSafely(this.exprs)) {
      return false;
    }

    Trigger parseHandler = CustomConditionSection.parserHandlers.get(which);

    if (parseHandler != null) {
      SyntaxParseEvent event = new SyntaxParseEvent(this.exprs, parseResult, ScriptLoader.getCurrentEvents());
      parseHandler.execute(event);

      if (SkriptReflection.hasLocalVariables(event)) {
        parseEvent = event;
      }

      return event.isMarkedContinue();
    }

    return true;
  }
}
