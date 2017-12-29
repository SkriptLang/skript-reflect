package com.btk5h.skriptmirror.skript.custom;

import org.bukkit.event.Event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;

public class EffDelayEffect extends Effect {
  static {
    Skript.registerEffect(EffDelayEffect.class, "delay [the] [current] effect");
  }

  @Override
  protected void execute(Event e) {
    ((CustomEffect.EffectEvent) e).setSync(false);
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "delay effect";
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    if (!ScriptLoader.isCurrentEvent(CustomEffect.EffectEvent.class)) {
      Skript.error("The effect 'delay effect' may only be used in a custom effect.",
          ErrorQuality.SEMANTIC_ERROR);
      return false;
    }
    return true;
  }
}
