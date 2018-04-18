package com.btk5h.skriptmirror.skript.custom.effect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;

import java.util.Arrays;

public class CustomEffect extends Effect {
  private String which;
  private Expression<?>[] exprs;
  private SkriptParser.ParseResult parseResult;

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
    EffectTriggerEvent effectEvent = new EffectTriggerEvent(e, exprs, parseResult, which, getNext());
    if (trigger == null) {
      Skript.error(String.format("The custom effect '%s' no longer has a handler.", which));
    } else {
      trigger.execute(effectEvent);
    }
    return effectEvent;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return which;
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    which = CustomEffectSection.effects.get(matchedPattern);
    this.exprs = Arrays.stream(exprs)
        .map(Util::defendExpression)
        .toArray(Expression[]::new);
    this.parseResult = parseResult;

    return Util.canInitSafely(this.exprs);
  }
}
