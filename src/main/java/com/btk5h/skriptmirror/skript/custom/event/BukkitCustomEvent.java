package com.btk5h.skriptmirror.skript.custom.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitCustomEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

}
