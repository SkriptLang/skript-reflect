package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SelfRegisteringSkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.WrappedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;

import java.util.Arrays;
import java.util.stream.Collectors;

public class EvtByReflection extends SelfRegisteringSkriptEvent {

  static {
    Skript.registerEvent("Bukkit", EvtByReflection.class, BukkitEvent.class,
        "[(1¦all)] %javatypes% [(at|on|with) priority <.+>]");
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
        trigger.execute(new BukkitEvent(event));
      }
    }
  }

  private static class BukkitEvent extends WrappedEvent implements Cancellable {
    public BukkitEvent(Event event) {
      super(event, event.isAsynchronous());
    }

    @Override
    public HandlerList getHandlers() {
      // No HandlerList implementation because this event should never be called
      throw new IllegalStateException();
    }

    @Override
    public boolean isCancelled() {
      Event event = getDirectEvent();
      return event instanceof Cancellable && ((Cancellable) event).isCancelled();
    }

    @Override
    public void setCancelled(boolean cancel) {
      Event event = getDirectEvent();
      if (event instanceof Cancellable) {
        ((Cancellable) event).setCancelled(cancel);
      }
    }
  }

  private Class<? extends Event>[] classes;
  private EventPriority priority;
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

    if (parseResult.regexes.size() > 0) {
      String priorityName = parseResult.regexes.get(0).group().toUpperCase();
      try {
        priority = EventPriority.valueOf(priorityName);
      } catch (IllegalArgumentException ex) {
        Skript.error(priorityName + " is not a valid priority level");
        return false;
      }
    } else {
      priority = SkriptConfig.defaultEventPriority.value();
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
          .registerEvent(eventClass, listener, priority, executor, SkriptMirror.getInstance(), ignoreCancelled);
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

  @Override
  public String toString(Event e, boolean debug) {
    return (ignoreCancelled ? "all " : "")
        + Arrays.stream(classes)
        .map(Class::getSimpleName)
        .collect(Collectors.joining(", "))
        + " with priority " + priority;
  }

}
