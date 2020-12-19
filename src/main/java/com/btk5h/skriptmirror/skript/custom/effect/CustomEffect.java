package com.btk5h.skriptmirror.skript.custom.effect;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class CustomEffect extends Effect {
  private EffectSyntaxInfo which;
  private Expression<?>[] exprs;
  private SkriptParser.ParseResult parseResult;
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

    Object localVars = SkriptReflection.getLocals(effectEvent.getDirectEvent());
    new Thread(() -> {
      try {
        Thread.sleep(1);
        if (!effectEvent.hasContinued())
          SkriptReflection.putLocals(localVars, effectEvent.getDirectEvent());
      } catch (InterruptedException ignored) { }
    }).start();
    return null;
  }

  private EffectTriggerEvent invokeEffect(Event e) {
    Trigger trigger = CustomEffectSection.effectHandlers.get(which);
    EffectTriggerEvent effectEvent =
        new EffectTriggerEvent(e, exprs, which.getMatchedPattern(), parseResult, which.getPattern(), getNext());
    if (trigger == null) {
      Skript.error(String.format("The custom effect '%s' no longer has a handler.", which));
    } else {
      SkriptReflection.putLocals(SkriptReflection.copyLocals(variablesMap), effectEvent);
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

    List<Supplier<Boolean>> suppliers = CustomEffectSection.usableSuppliers.get(which);
    if (suppliers != null && suppliers.size() != 0 && suppliers.stream().noneMatch(Supplier::get))
      return false;

    Boolean bool = CustomEffectSection.parseSectionLoaded.get(which);
    if (bool != null && !bool) {
      Skript.error("You can't use custom effects with parse sections before they're loaded.");
      return false;
    }

    Trigger parseHandler = CustomEffectSection.parserHandlers.get(which);

    if (parseHandler != null) {
      SyntaxParseEvent event =
          new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, ScriptLoader.getCurrentEvents());

      TriggerItem.walk(parseHandler, event);
      variablesMap = SkriptReflection.removeLocals(event);

      return event.isMarkedContinue();
    }

    return true;
  }
}
