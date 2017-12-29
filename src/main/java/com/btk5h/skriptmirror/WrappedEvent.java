package com.btk5h.skriptmirror;

import org.bukkit.event.Event;

public abstract class WrappedEvent extends Event {
  private final Event event;

  protected WrappedEvent(Event event) {
    this.event = event;
  }

  public Event getEvent() {
    return event instanceof WrappedEvent ? ((WrappedEvent) event).getEvent() : event;
  }

  public Event getDirectEvent() {
    return event;
  }
}
