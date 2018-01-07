package com.btk5h.skriptmirror.skript;

import com.btk5h.skriptmirror.Util;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;

public class EffTerminatedLine extends Effect {
  static {
    Skript.registerEffect(EffTerminatedLine.class, "%objects%;");
  }

  private Expression<Object> arg;

  @Override
  protected void execute(Event e) {
    arg.getAll(e);
  }

  @Override
  public String toString(Event e, boolean debug) {
    return arg.toString(e, debug);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    arg = Util.defendExpression(exprs[0]);
    return Util.canInitSafely(arg);
  }
}
