package com.btk5h.skriptmirror.skript;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;

public class LitTrue extends SimpleLiteral<Boolean> {
  static {
    Skript.registerExpression(LitTrue.class, Boolean.class, ExpressionType.SIMPLE, "(true|yes|on)");
  }

  public LitTrue() {
    super(true, false);
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    return true;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "true";
  }
}
