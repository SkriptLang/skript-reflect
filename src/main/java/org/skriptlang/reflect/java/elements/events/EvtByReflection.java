package org.skriptlang.reflect.java.elements.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
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

public class EvtByReflection extends SkriptEvent {

	static {
		Skript.registerEvent("*reflection", EvtByReflection.class, BukkitEvent.class, "%javatypes%");
	}

	private static class MyEventExecutor implements EventExecutor {
		private final Class<? extends Event> eventClass;
		private final ListeningBehavior listeningBehavior;
		private final Trigger trigger;

		public MyEventExecutor(Class<? extends Event> eventClass, Trigger trigger) {
			this.eventClass = eventClass;
			this.listeningBehavior = ListeningBehavior.UNCANCELLED;
			this.trigger = trigger;
		}

		public MyEventExecutor(Class<? extends Event> eventClass, ListeningBehavior listeningBehavior, Trigger trigger) {
			this.eventClass = eventClass;
			this.listeningBehavior = listeningBehavior;
			this.trigger = trigger;
		}

		@Override
		public void execute(Listener listener, Event event) throws EventException {
			if (eventClass.isInstance(event)) {
				if (event instanceof Cancellable && listeningBehavior != null && !listeningBehavior.matches(((Cancellable) event).isCancelled()))
					return;

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
	private Listener listener;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
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

		listener = new Listener() {};

		return true;
	}

	@Override
	public boolean check(Event event) {
		throw new UnsupportedOperationException(); // Should never be called
	}

	@Override
	public boolean postLoad() {
		for (Class<? extends Event> eventClass : classes) {
			EventExecutor executor = new MyEventExecutor(eventClass, listeningBehavior, trigger);

			Bukkit.getPluginManager()
					.registerEvent(eventClass, listener, getEventPriority(), executor, SkriptMirror.getInstance(), listeningBehavior == ListeningBehavior.UNCANCELLED);
		}
		return true;
	}

	public void unload() {
		HandlerList.unregisterAll(listener);
	}

	@Override
	public boolean isListeningBehaviorSupported() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends Event>[] getEventClasses() {
		boolean hasUncancellable = false;
		boolean hasCancellable = false;

		if (classes == null)
			return new Class[]{BukkitEvent.class};

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
	public String toString(Event e, boolean debug) {
		return Arrays.stream(classes)
				.map(Class::getSimpleName)
				.collect(Collectors.joining(", "));
	}

}
