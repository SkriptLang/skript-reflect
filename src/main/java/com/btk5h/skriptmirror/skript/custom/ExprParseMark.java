package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.condition.ConditionCheckEvent;
import com.btk5h.skriptmirror.skript.custom.effect.EffectTriggerEvent;
import com.btk5h.skriptmirror.skript.custom.event.EventTriggerEvent;
import com.btk5h.skriptmirror.skript.custom.expression.ExpressionChangeEvent;
import com.btk5h.skriptmirror.skript.custom.expression.ExpressionGetEvent;
import org.bukkit.event.Event;

import java.util.Arrays;

public class ExprParseMark extends SimpleExpression<Number> {
  static {
    Skript.registerExpression(ExprParseMark.class, Number.class, ExpressionType.SIMPLE,
        "[the] [parse[r]] mark");
  }

  @Override
  protected Number[] get(Event e) {
    return new Number[]{((CustomSyntaxEvent) e).getParseResult().mark};
  }

  @Override
  public boolean isSingle() {
    return true;
  }

  @Override
  public Class<? extends Number> getReturnType() {
    return Number.class;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "parser mark";
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    if (!ScriptLoader.isCurrentEvent(
      SyntaxParseEvent.class,
      ConditionCheckEvent.class,
      EffectTriggerEvent.class,
      EventTriggerEvent.class,
      ExpressionChangeEvent.class,
      ExpressionGetEvent.class
    )) {
      Skript.error("The parser mark may only be used in custom syntax.", ErrorQuality.SEMANTIC_ERROR);
      return false;
    }
    return true;
  }
}
