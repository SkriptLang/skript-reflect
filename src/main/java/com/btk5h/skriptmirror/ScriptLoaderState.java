package com.btk5h.skriptmirror;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Config;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptReflection;
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
    SkriptReflection.setCurrentScript(currentScript);
    ScriptLoader.setCurrentEvent(currentEventName, currentEvents);
    SkriptReflection.setHasDelayBefore(hasDelayBefore);
  }

  public static ScriptLoaderState copyOfCurrentState() {
    return new ScriptLoaderState(
      SkriptReflection.getCurrentScript(),
      ScriptLoader.getCurrentEventName(),
      ScriptLoader.getCurrentEvents(),
      SkriptReflection.getHasDelayBefore()
    );
  }
}
