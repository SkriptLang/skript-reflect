package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SelfRegisteringSkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.WrappedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.util.Arrays;
import java.util.stream.Collectors;

public class EvtByReflection extends SelfRegisteringSkriptEvent {

  static {
    Skript.registerEvent("*reflection", EvtByReflection.class, BukkitEvent.class,
        "[(1¦all)] %javatypes%");
  }

  private static class MyEventExecutor implements EventExecutor {
    private final Class<? extends Event> eventClass;
    private final Trigger trigger;

    public MyEventExecutor(Class<? extends Event> eventClass, Trigger trigger) {
      this.eventClass = eventClass;
      this.trigger = trigger;
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
      if (eventClass.isInstance(event)) {
        Event scriptEvent;
        scriptEvent = event instanceof Cancellable
            ? new CancellableBukkitEvent((Cancellable) event) : new BukkitEvent(event);

        trigger.execute(scriptEvent);
      }
    }
  }

  private static class BukkitEvent extends WrappedEvent {
    public BukkitEvent(Event event) {
      super(event, event.isAsynchronous());
    }

    @Override
    public HandlerList getHandlers() {
      // No HandlerList implementation because this event should never be called
      throw new IllegalStateException();
    }
  }

  private static class CancellableBukkitEvent extends BukkitEvent implements Cancellable {
    public CancellableBukkitEvent(Cancellable event) {
      super((Event) event);
    }

    @Override
    public boolean isCancelled() {
      Event event = getDirectEvent();
      return ((Cancellable) event).isCancelled();
    }

    @Override
    public void setCancelled(boolean cancel) {
      Event event = getDirectEvent();
      ((Cancellable) event).setCancelled(cancel);
    }
  }

  private Class<? extends Event>[] classes;
  private boolean ignoreCancelled;
  private Listener listener;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
    JavaType[] javaTypes = ((Literal<JavaType>) args[0]).getArray();

    classes = new Class[javaTypes.length];

    for (int i = 0; i < javaTypes.length; i++) {
      JavaType javaType = javaTypes[i];
      Class<?> clazz = javaType.getJavaClass();

      if (!Event.class.isAssignableFrom(clazz)) {
        Skript.error(clazz.getSimpleName() + " is not a Bukkit event");
        return false;
      }

      classes[i] = (Class<? extends Event>) clazz;
    }

    ignoreCancelled = (parseResult.mark & 1) != 1;

    listener = new Listener() {};

    return true;
  }

  @Override
  public void register(Trigger t) {
    for (Class<? extends Event> eventClass : classes) {
      EventExecutor executor = new MyEventExecutor(eventClass, t);

      Bukkit.getPluginManager()
          .registerEvent(eventClass, listener, getEventPriority(), executor, SkriptMirror.getInstance(), ignoreCancelled);
    }
  }

  @Override
  public void unregister(Trigger t) {
    HandlerList.unregisterAll(listener);
  }

  @Override
  public void unregisterAll() {
    HandlerList.unregisterAll(listener);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<? extends Event>[] getEventClasses() {
    boolean hasUncancellable = false;
    boolean hasCancellable = false;

    for (Class<? extends Event> eventClass : classes) {
      if (Cancellable.class.isAssignableFrom(eventClass)) {
        hasCancellable = true;
      } else {
        hasUncancellable = true;
      }
    }

    if (hasCancellable && hasUncancellable) {
      return new Class[] {BukkitEvent.class, CancellableBukkitEvent.class};
    } else if (hasCancellable) {
      return new Class[] {CancellableBukkitEvent.class};
    } else {
      return new Class[] {BukkitEvent.class};
    }
  }

  @Override
  public boolean isEventPrioritySupported() {
    return true;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return (ignoreCancelled ? "all " : "")
        + Arrays.stream(classes)
        .map(Class::getSimpleName)
        .collect(Collectors.joining(", "));
  }

}
