package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Call Event")
@Description({"You can find more information about this here: https://tpgamesnl.gitbook.io/skript-reflect/advanced/custom-syntax/events#calling-the-event"})
public class EffCallEvent extends Effect {

  static {
    Skript.registerEffect(EffCallEvent.class, "call [event] %events%");
  }

  private Expression<Event> eventExpr;

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    eventExpr = SkriptUtil.defendExpression(exprs[0]);
    return SkriptUtil.canInitSafely(eventExpr);
  }

  @Override
  protected void execute(Event e) {
    for (Event event : eventExpr.getArray(e))
      Bukkit.getPluginManager().callEvent(event);
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return "call event " + eventExpr.toString(e, debug);
  }

}
