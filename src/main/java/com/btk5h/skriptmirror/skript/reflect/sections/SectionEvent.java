package com.btk5h.skriptmirror.skript.reflect.sections;

import com.btk5h.skriptmirror.WrappedEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SectionEvent extends WrappedEvent {

  private final static HandlerList handlers = new HandlerList();
  private final Section section;

  public SectionEvent(Event event, Section section) {
    super(event);
    this.section = section;
  }

  public void setOutput(Object[] output) {
    section.setOutput(output);
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

}
