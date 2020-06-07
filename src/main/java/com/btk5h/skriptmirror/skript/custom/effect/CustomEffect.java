package com.btk5h.skriptmirror.skript.custom.effect;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.util.Arrays;

public class CustomEffect extends Effect {
  private EffectSyntaxInfo which;
  private Expression<?>[] exprs;
  private SkriptParser.ParseResult parseResult;
  private Event parseEvent;
  private Object variablesMap;

  @Override
  protected void execute(Event e) {
    // for effect commands
    invokeEffect(e);
  }

  @Override
  protected TriggerItem walk(Event e) {
    EffectTriggerEvent effectEvent = invokeEffect(e);

    if (effectEvent.isSync()) {
      return getNext();
    }

    return null;
  }

  private EffectTriggerEvent invokeEffect(Event e) {
    Trigger trigger = CustomEffectSection.effectHandlers.get(which);
    EffectTriggerEvent effectEvent =
        new EffectTriggerEvent(e, exprs, which.getMatchedPattern(), parseResult, which.getPattern(), getNext());
    if (trigger == null) {
      Skript.error(String.format("The custom effect '%s' no longer has a handler.", which));
    } else {
      SkriptReflection.copyVariablesMapFromMap(variablesMap, effectEvent);
      trigger.execute(effectEvent);
    }
    return effectEvent;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return which.getPattern();
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    which = CustomEffectSection.lookup(SkriptUtil.getCurrentScript(), matchedPattern);

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

    Trigger parseHandler = CustomEffectSection.parserHandlers.get(which);

    if (parseHandler != null) {
      SyntaxParseEvent event =
          new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, ScriptLoader.getCurrentEvents());

      // Because of link below, Trigger#execute removes local variables
      // https://github.com/SkriptLang/Skript/commit/a6661c863bae65e96113b69bebeaab51d814e2b9
      TriggerItem.walk(parseHandler, event);
      variablesMap = Variables.removeLocals(event);

      return event.isMarkedContinue();
    }

    return true;
  }
}
