package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;

@SuppressWarnings("unused")
public class ExprCustomEventValue<T> extends EventValueExpression<T> {

  static {
    //noinspection unchecked
    Skript.registerExpression(ExprCustomEventValue.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, "[the] [event-]<.+>");
  }

  private ClassInfo<?> classInfo;

  @SuppressWarnings("unchecked")
  public ExprCustomEventValue() {
    super((Class<? extends T>) Object.class);
  }

  @Override
  public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final SkriptParser.ParseResult parseResult) {
    if (!ScriptLoader.isCurrentEvent(BukkitCustomEvent.class))
      return false;
    CustomEvent customEvent = CustomEvent.getLastCustomEvent();
    if (customEvent == null)
      return false;

    String stringClass = parseResult.regexes.get(0).group();
    classInfo = Classes.getClassInfoFromUserInput(stringClass);
    if (classInfo == null) {
      return false;
    }

    if (!CustomEventUtils.hasEventValue(customEvent, classInfo)) {
      Skript.error("There is no " + CustomEventUtils.getName(classInfo) + " in the custom event " +
        CustomEventUtils.getName(customEvent));
      return false;
    }

    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T[] get(Event event) {
    if (!(event instanceof BukkitCustomEvent))
      return null;
    BukkitCustomEvent bukkitCustomEvent = (BukkitCustomEvent) event;

    T[] tArray = (T[]) Array.newInstance(classInfo.getC(), 1);
    tArray[0] = (T) bukkitCustomEvent.getEventValue(classInfo);
    return tArray;
  }

  @Override
  public String toString(final @Nullable Event e, final boolean debug) {
    return "event-" + this.getReturnType();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<T> getReturnType() {
    return (Class<T>) classInfo.getC();
  }

}
