package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.registrations.Classes;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;

/**
 * This class is a wrapper for default expression from event-values.
 *
 * @param <T> The return type
 */
public class ExprReplacedEventValue<T> extends EventValueExpression<T> {

  private final EventValueExpression<T> original;

  public ExprReplacedEventValue(EventValueExpression<T> original) {
    super(original.getReturnType());
    this.original = original;
  }

  @SuppressWarnings("unchecked")
  @Override
  @Nullable
  protected T[] get(final Event e) {
    if (e instanceof BukkitCustomEvent || e instanceof EventTriggerEvent) {
      BukkitCustomEvent bukkitCustomEvent;
      if (e instanceof BukkitCustomEvent) {
        bukkitCustomEvent = (BukkitCustomEvent) e;
      } else {
        bukkitCustomEvent = (BukkitCustomEvent) ((EventTriggerEvent) e).getDirectEvent();
      }

      Class<?> valueClass = original.getReturnType();

      T[] tArray = (T[]) Array.newInstance(valueClass, 1);
      tArray[0] = (T) bukkitCustomEvent.getEventValue(Classes.getSuperClassInfo(valueClass));
      return tArray;
    } else {
      return original.getArray(e);
    }
  }

  @SuppressWarnings("null")
  @Override
  public boolean init() {
    if (ScriptLoader.isCurrentEvent(BukkitCustomEvent.class, EventTriggerEvent.class)) {
      EventSyntaxInfo which = CustomEvent.lastWhich;
      ClassInfo<?> classInfo = Classes.getSuperClassInfo(getReturnType());
      return CustomEventUtils.hasEventValue(which, classInfo);
    } else {
      return original.init();
    }
  }

  @Override
  public String toString(final @Nullable Event e, final boolean debug) {
    return original.toString(e, debug);
  }

  @Override
  @Nullable
  public Class<?>[] acceptChange(final Changer.ChangeMode mode) {
    return original.acceptChange(mode);
  }

  @Override
  public void change(final Event e, final @Nullable Object[] delta, final Changer.ChangeMode mode) {
    original.change(e, delta, mode);
  }

  @Override
  public boolean setTime(final int time) {
    return original.setTime(time);
  }

}
