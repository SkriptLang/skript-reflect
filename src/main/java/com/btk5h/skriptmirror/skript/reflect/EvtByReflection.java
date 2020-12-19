package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.SkriptEventHandler;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.WrappedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EvtByReflection extends SkriptEvent {
  static {
    Skript.registerEvent("Bukkit Event", EvtByReflection.class, BukkitEvent.class,
        "[(1¦all)] %javatypes% [(at|on|with) priority <.+>]");
  }

  private static class PriorityListener implements Listener {
    private final EventPriority priority;
    private final Set<Class<? extends Event>> events = new HashSet<>();

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

  private static final EventExecutor executor = (listener, event) -> Bukkit.getPluginManager()
    .callEvent(new BukkitEvent(event, ((PriorityListener) listener).getPriority()));

  private static final PriorityListener[] listeners;

  static {
    SkriptEventHandler.listenCancelled.add(BukkitEvent.class);

    listeners = Arrays.stream(EventPriority.values())
      .mapToInt(EventPriority::ordinal)
      .mapToObj(PriorityListener::new)
      .toArray(PriorityListener[]::new);
  }

  private static class BukkitEvent extends WrappedEvent implements Cancellable {
    private final static HandlerList handlers = new HandlerList();

    private final EventPriority priority;

    public BukkitEvent(Event event, EventPriority priority) {
      super(event, event.isAsynchronous());
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

  private static void registerEvent(Class<? extends Event> event, EventPriority priority) {
    PriorityListener listener = listeners[priority.ordinal()];
    Set<Class<? extends Event>> events = listener.getEvents();

    if (!events.contains(event)) {
      events.add(event);
      Bukkit.getPluginManager()
        .registerEvent(event, listener, priority, executor, SkriptMirror.getInstance(), false);
    }

  }

  private Class<? extends Event>[] classes;
  private EventPriority priority;
  private boolean ignoreCancelled;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
    classes = Arrays.stream(((Literal<JavaType>) args[0]).getArray())
        .map(JavaType::getJavaClass)
        .toArray(Class[]::new);

    if (parseResult.regexes.size() > 0) {
      String priorityName = parseResult.regexes.get(0).group().toUpperCase();
      try {
        priority = EventPriority.valueOf(priorityName);
      } catch (IllegalArgumentException ex) {
        Skript.error(priorityName + " is not a valid priority level.");
        return false;
      }
    } else {
      priority = SkriptConfig.defaultEventPriority.value();
    }

    ignoreCancelled = (parseResult.mark & 1) != 1;

    for (Class<? extends Event> cls : classes) {
      registerEvent(cls, priority);
    }

    return true;
  }

  @Override
  public boolean check(Event e) {
    Event extractedEvent = ((BukkitEvent) e).getEvent();
    Class<? extends Event> eventClass = extractedEvent.getClass();

    if (extractedEvent instanceof Cancellable && ((Cancellable) extractedEvent).isCancelled() && ignoreCancelled)
      return false;

    if (priority == ((BukkitEvent) e).getPriority()) {
      for (Class<? extends Event> cls : classes) {
        if (cls.isAssignableFrom(eventClass)) {
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
