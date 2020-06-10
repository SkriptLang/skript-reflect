package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class EffCallCustomEvent extends Effect {

  static {
    Skript.registerEffect(EffCallCustomEvent.class, "call custom event %string% [(with|using) [[event-]values] %-objects%] [[and] [(with|using)] data %-objects%]");
  }

  private Expression<String> customEventName;
  private Variable<?> eventValueVarList;
  private Variable<?> dataVarList;

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    this.customEventName = SkriptUtil.defendExpression(exprs[0]);
    if (exprs[1] != null) {
      Expression<?> var = SkriptUtil.defendExpression(exprs[1]);

      if (var instanceof Variable && ((Variable<?>) var).isList()) {
        this.eventValueVarList = (Variable<?>) var;
      } else {
        Skript.error(var.toString() + " is not a list variable.");
        return false;
      }
    }

    if (exprs[2] != null) {
      Expression<?> var = SkriptUtil.defendExpression(exprs[2]);

      if (var instanceof Variable && ((Variable<?>) var).isList()) {
        this.dataVarList = (Variable<?>) var;
      } else {
        Skript.error(var.toString() + " is not a list variable.");
        return false;
      }
    }

    return true;
  }

  @Override
  protected void execute(Event e) {
    BukkitCustomEvent bukkitCustomEvent = new BukkitCustomEvent(this.customEventName.getSingle(e));

    if (eventValueVarList != null)
      eventValueVarList.variablesIterator(e).forEachRemaining(pair -> {
        if (pair.getKey() == null)
          return;
        ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(pair.getKey());
        bukkitCustomEvent.setEventValue(classInfo, pair.getValue());
      });

    if (dataVarList != null)
      dataVarList.variablesIterator(e).forEachRemaining(pair -> {
        bukkitCustomEvent.setData(pair.getKey(), pair.getValue());
      });

    Bukkit.getPluginManager().callEvent(bukkitCustomEvent);
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return "call custom event \"" + this.customEventName + "\"";
  }

}
