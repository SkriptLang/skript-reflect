package com.btk5h.skriptmirror.skript.custom.effect;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;

import java.util.*;

public class CustomEffectSection extends SelfRegisteringSkriptEvent {
  static {
    //noinspection unchecked
    Skript.registerEvent("*Define Effect", CustomEffectSection.class, EffectTriggerEvent.class, "effect <.+>");

    Skript.registerEffect(CustomEffect.class);
    Optional<SyntaxElementInfo<? extends Effect>> info = Skript.getEffects().stream()
        .filter(i -> i.c == CustomEffect.class)
        .findFirst();

    if (info.isPresent()) {
      thisInfo = info.get();
    } else {
      Skript.warning("Could not find custom effect class. Custom effects will not work.");
    }
  }

  private static SyntaxElementInfo<?> thisInfo;
  private static final SectionValidator EFFECT_DECLARATION = new SectionValidator()
      .addSection("trigger", false);

  static final List<String> effects = new ArrayList<>();
  static final Map<String, Trigger> effectHandlers = new HashMap<>();

  private String which;

  @Override
  public boolean init(Literal<?>[] args, int matchedPattern,
                      SkriptParser.ParseResult parseResult) {
    which = Util.preprocessPattern(parseResult.regexes.get(0).group());
    if (effects.contains(which)) {
      Skript.error(String.format("The custom effect '%s' already has a handler.", which));
      return false;
    }
    effects.add(which);

    SectionNode node = (SectionNode) SkriptLogger.getNode();
    node.convertToEntries(0);

    boolean ok = EFFECT_DECLARATION.validate(node);

    if (!ok) {
      unregister(null);
      return false;
    }

    register(node);
    return true;
  }

  @SuppressWarnings("unchecked")
  private void register(SectionNode node) {
    ScriptLoader.setCurrentEvent("custom effect trigger", EffectTriggerEvent.class);
    Util.getItemsFromNode(node, "trigger").ifPresent(items ->
      effectHandlers.put(which, new Trigger(ScriptLoader.currentScript.getFile(), "effect " + which, this, items))
    );

    Util.clearSectionNode(node);
    updateEffects();
  }

  @Override
  public void register(Trigger t) {
  }

  @Override
  public void unregister(Trigger t) {
    effects.remove(which);
    effectHandlers.remove(which);
    updateEffects();
  }

  @Override
  public void unregisterAll() {
    effects.clear();
    effectHandlers.clear();
    updateEffects();
  }

  @Override
  public String toString(Event e, boolean debug) {
    return which;
  }

  private static void updateEffects() {
    Util.setPatterns(thisInfo, effects.toArray(new String[0]));
  }
}
