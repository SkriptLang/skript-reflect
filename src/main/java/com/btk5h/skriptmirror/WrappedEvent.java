package com.btk5h.skriptmirror;

import org.bukkit.event.Event;

public abstract class WrappedEvent extends Event {
	private final Event event;

	protected WrappedEvent(Event event) {
		this.event = event;
	}

	protected WrappedEvent(Event event, boolean isAsynchronous) {
		super(isAsynchronous);
		this.event = event;
	}

	public Event getEvent() {
		if (event instanceof WrappedEvent) {
			return ((WrappedEvent) event).getEvent();
		}
		return event;
	}

	public Event getDirectEvent() {
		return event;
	}

}
