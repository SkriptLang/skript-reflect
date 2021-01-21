package com.btk5h.skriptmirror;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Config;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

public class ScriptLoaderState {
  private final Config currentScript;
  private final String currentEventName;
  private final Class<? extends Event>[] currentEvents;
  private final Kleenean hasDelayBefore;

  private ScriptLoaderState(Config currentScript, String currentEventName, Class<? extends Event>[] currentEvents,
                            Kleenean hasDelayBefore) {
    this.currentScript = currentScript;
    this.currentEventName = currentEventName;
    this.currentEvents = currentEvents;
    this.hasDelayBefore = hasDelayBefore;
  }

  public void applyToCurrentState() {
    ScriptLoader.currentScript = currentScript;
    ScriptLoader.setCurrentEvent(currentEventName, currentEvents);
    ScriptLoader.hasDelayBefore = hasDelayBefore;
  }

  public static ScriptLoaderState copyOfCurrentState() {
    return new ScriptLoaderState(
      ScriptLoader.currentScript,
      ScriptLoader.getCurrentEventName(),
      ScriptLoader.getCurrentEvents(),
      ScriptLoader.hasDelayBefore
    );
  }
}
