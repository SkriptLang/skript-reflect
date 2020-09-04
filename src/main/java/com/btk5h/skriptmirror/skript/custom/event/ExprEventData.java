package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;

@Name("Event Data")
@Description({"You can find more information about this here: https://tpgamesnl.gitbook.io/skript-reflect/advanced/custom-syntax/events#extra-data"})
public class ExprEventData extends SimpleExpression<Object> {

  static {
    Skript.registerExpression(ExprEventData.class, Object.class, ExpressionType.COMBINED, "[extra] [event[-]] data %strings%");
  }

  private Expression<String> dataIndex;

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    if (!ScriptLoader.isCurrentEvent(BukkitCustomEvent.class, EventTriggerEvent.class)) {
      Skript.error("This expression can only be used in a custom event");
      return false;
    }
    dataIndex = SkriptUtil.defendExpression(exprs[0]);
    return true;
  }

  @Nullable
  @Override
  protected Object[] get(Event e) {
    BukkitCustomEvent bukkitCustomEvent;
    if (e instanceof BukkitCustomEvent) {
      bukkitCustomEvent = (BukkitCustomEvent) e;
    } else {
      bukkitCustomEvent = (BukkitCustomEvent) ((EventTriggerEvent) e).getEvent();
    }

    if (dataIndex.isSingle()) {
      Object data = bukkitCustomEvent.getData(dataIndex.getSingle(e));
      return new Object[] {data};
    } else {
      ArrayList<Object> arrayList = new ArrayList<>();
      for (String index : dataIndex.getArray(e)) {
        arrayList.add(bukkitCustomEvent.getData(index));
      }
      return arrayList.toArray();
    }
  }

  @Override
  public boolean isSingle() {
    return dataIndex.isSingle();
  }

  @Override
  public Class<?> getReturnType() {
    return Object.class;
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return "event data";
  }

}
