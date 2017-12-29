package com.btk5h.skriptmirror.skript.custom;

import org.bukkit.event.Event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;

public class ExprChangeMode extends SimpleExpression<Changer.ChangeMode> {
  static {
    Skript.registerExpression(ExprChangeMode.class, Changer.ChangeMode.class, ExpressionType.SIMPLE,
        "[the] change mode");
  }

  @Override
  protected Changer.ChangeMode[] get(Event e) {
    return new Changer.ChangeMode[]{((CustomExpression.ExpressionChangeEvent) e).getMode()};
  }

  @Override
  public boolean isSingle() {
    return true;
  }

  @Override
  public Class<? extends Changer.ChangeMode> getReturnType() {
    return Changer.ChangeMode.class;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "change mode";
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    if (!ScriptLoader.isCurrentEvent(CustomExpression.ExpressionChangeEvent.class)) {
      Skript.error("The change mode may only be used in change handlers.",
          ErrorQuality.SEMANTIC_ERROR);
    }
    return true;
  }
}
