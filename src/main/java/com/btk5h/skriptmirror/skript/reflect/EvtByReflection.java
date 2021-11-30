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
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EvtByReflection extends SkriptEvent {

  static {
    Skript.registerEvent("Bukkit", EvtByReflection.class, BukkitEvent.class,
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

  private static class TypeStrictEventExecutor implements EventExecutor {

    private final Class<? extends Event> eventClass;

    public TypeStrictEventExecutor(Class<? extends Event> eventClass) {
      this.eventClass = eventClass;
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
      if (eventClass.isInstance(event))
        Bukkit.getPluginManager().callEvent(new BukkitEvent(event, ((PriorityListener) listener).getPriority()));
    }

  }

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

  private static void registerEvent(Class<? extends Event> eventClass, EventPriority priority) {
    PriorityListener listener = listeners[priority.ordinal()];
    Set<Class<? extends Event>> eventClasses = listener.getEvents();

    if (!eventClasses.contains(eventClass)) {
      eventClasses.add(eventClass);

      EventExecutor executor = new TypeStrictEventExecutor(eventClass);
      Bukkit.getPluginManager()
        .registerEvent(eventClass, listener, priority, executor, SkriptMirror.getInstance(), false);
    }
  }

  private Class<? extends Event>[] classes;
  private EventPriority priority;
  private boolean ignoreCancelled;

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
    Class<?>[] classArray = Arrays.stream(((Literal<JavaType>) args[0]).getArray())
      .map(JavaType::getJavaClass)
      .toArray(Class[]::new);

    for (Class<?> clazz : classArray) {
      if (!Event.class.isAssignableFrom(clazz)) {
        Skript.error(clazz.getSimpleName() + " is not a Bukkit event");
        return false;
      }
    }
    classes = (Class<? extends Event>[]) classArray;

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
    BukkitEvent bukkitEvent = (BukkitEvent) e;
    Event extractedEvent = bukkitEvent.getDirectEvent();
    Class<? extends Event> eventClass = extractedEvent.getClass();

    if (ignoreCancelled && extractedEvent instanceof Cancellable && ((Cancellable) extractedEvent).isCancelled())
      return false;

    if (priority == bukkitEvent.getPriority()) {
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
