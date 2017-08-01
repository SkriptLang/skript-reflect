package com.btk5h.skriptmirror.skript;

import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.WrappedEvent;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;

public class EvtByReflection extends SkriptEvent {
  static {
    Skript.registerEvent("Bukkit Event", EvtByReflection.class, BukkitEvent.class,
        "%strings% [(1¦(at|on|with) priority <.+>)]");
  }

  private static class PriorityListener implements Listener {
    private EventPriority priority;
    private Set<Class<? extends Event>> events = new HashSet<>();

    public PriorityListener(int priority) {
      this.priority = EventPriority.values()[priority];
    }

    public EventPriority getPriority() {
      return priority;
    }

    public Set<Class<? extends Event>> getEvents() {
      return events;
    }
  }

  private static EventExecutor executor =
      (listener, event) -> Bukkit.getPluginManager()
          .callEvent(new BukkitEvent(event, ((PriorityListener) listener).getPriority()));

  private static PriorityListener[] listeners = new PriorityListener[]{
      new PriorityListener(0),
      new PriorityListener(1),
      new PriorityListener(2),
      new PriorityListener(3),
      new PriorityListener(4),
      new PriorityListener(5)
  };

  private static class BukkitEvent extends WrappedEvent implements Cancellable {
    private final static HandlerList handlers = new HandlerList();

    private final EventPriority priority;

    public BukkitEvent(Event event, EventPriority priority) {
      super(event);
      this.priority = priority;
    }

    public EventPriority getPriority() {
      return priority;
    }

    public static HandlerList getHandlerList() {
      return handlers;
    }

    @Override
    public HandlerList getHandlers() {
      return handlers;
    }

    @Override
    public boolean isCancelled() {
      Event event = getEvent();
      return getEvent() instanceof Cancellable && ((Cancellable) event).isCancelled();
    }

    @Override
    public void setCancelled(boolean cancel) {
      Event event = getEvent();
      if (event instanceof Cancellable) {
        ((Cancellable) event).setCancelled(cancel);
      }
    }
  }

  private static Set<Class<? extends Event>> events = new HashSet<>();

  private static void registerEvent(Class<? extends Event> event, EventPriority priority) {
    PriorityListener listener = listeners[priority.ordinal()];
    Set<Class<? extends Event>> events = listener.getEvents();

    if (!events.contains(event)) {
      events.add(event);
      Bukkit.getPluginManager()
          .registerEvent(event, listener, priority, executor, SkriptMirror.getInstance());
    }
  }

  private Class<? extends Event>[] classes;
  private EventPriority priority;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
    String[] events = ((Literal<String>) args[0]).getArray();

    classes = (Class<? extends Event>[]) Array.newInstance(Class.class, events.length);

    for (int i = 0; i < events.length; i++) {
      String event = events[i];

      try {
        Class<?> eventClass = Class.forName(event);

        if (!Event.class.isAssignableFrom(eventClass)) {
          Skript.error(event + " is not an event.");
          return false;
        }

        classes[i] = (Class<? extends Event>) eventClass;
      } catch (ClassNotFoundException e) {
        Skript.error(event + " refers to a non-existent class.");
        return false;
      }
    }

    if (parseResult.mark == 0) {
      priority = SkriptConfig.defaultEventPriority.value();
    } else {
      String priorityName = parseResult.regexes.get(0).group().toUpperCase();
      try {
        priority = EventPriority.valueOf(priorityName);
      } catch (IllegalArgumentException ex) {
        Skript.error(priorityName + " is not a valid priority level.");
        return false;
      }
    }

    for (Class<? extends Event> cls : classes) {
      registerEvent(cls, priority);
    }

    return true;
  }

  @Override
  public boolean check(Event e) {
    Class<? extends Event> eventClass = ((BukkitEvent) e).getEvent().getClass();
    if (priority == ((BukkitEvent) e).getPriority()) {
      for (Class<? extends Event> cls : classes) {
        if (cls == eventClass) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return Arrays.toString(classes) + " priority " + priority;
  }
}
