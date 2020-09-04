package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.condition.ConditionCheckEvent;
import com.btk5h.skriptmirror.skript.custom.effect.EffectTriggerEvent;
import com.btk5h.skriptmirror.skript.custom.event.EventTriggerEvent;
import org.bukkit.event.Event;

@Name("Continue")
@Description({"For more information on skript-reflect, you can visit the wiki here: https://tpgamesnl.gitbook.io/skript-reflect/"})
public class EffContinue extends Effect {
  static {
    Skript.registerEffect(EffContinue.class, "continue");
  }

  @Override
  protected void execute(Event e) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected TriggerItem walk(Event e) {
    if (e instanceof EffectTriggerEvent) {
      if (((EffectTriggerEvent) e).isSync()) {
        Skript.warning("Synchronous events should not be continued. " +
            "Call 'delay effect' to delay the effect's execution.");
      } else {
        EffectTriggerEvent effectTriggerEvent = (EffectTriggerEvent) e;
        effectTriggerEvent.setContinued();
        TriggerItem.walk(effectTriggerEvent.getNext(), effectTriggerEvent.getDirectEvent());
      }
      // TODO make ContinuableEvent interface / abstract class or sth alike
    } else if (e instanceof ConditionCheckEvent) {
      ((ConditionCheckEvent) e).markContinue();
    } else if (e instanceof SyntaxParseEvent) {
      ((SyntaxParseEvent) e).markContinue();
    } else if (e instanceof EventTriggerEvent) {
      ((EventTriggerEvent) e).markContinue();
    }

    return null;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "continue";
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    if (!ScriptLoader.isCurrentEvent(EffectTriggerEvent.class, ConditionCheckEvent.class, SyntaxParseEvent.class, EventTriggerEvent.class)) {
      Skript.error("Return may only be used in custom effects and conditions.", ErrorQuality.SEMANTIC_ERROR);
      return false;
    }

    return true;
  }
}
