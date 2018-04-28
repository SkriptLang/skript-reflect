package com.btk5h.skriptmirror.skript.custom.effect;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import com.btk5h.skriptmirror.Util;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CustomEffectSection extends CustomSyntaxSection<SyntaxInfo> {
  static {
    //noinspection unchecked
    CustomSyntaxSection.register("Define Effect", CustomEffectSection.class,
        "[(1¦local)] effect <.+>",
        "[(1¦local)] effect");
  }

  private static DataTracker<SyntaxInfo> dataTracker = new DataTracker<>();

  static final Map<SyntaxInfo, Trigger> effectHandlers = new HashMap<>();

  static {
    dataTracker.setSyntaxType("effect");

    dataTracker.getValidator()
        .addSection("trigger", false)
        .addSection("patterns", true);

    Skript.registerEffect(CustomEffect.class);
    Optional<SyntaxElementInfo<? extends Effect>> info = Skript.getEffects().stream()
        .filter(i -> i.c == CustomEffect.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(effectHandlers);
  }
  @Override
  public DataTracker<SyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         SectionNode node) {
    SectionNode patterns = (SectionNode) node.get("patterns");
    File script = (parseResult.mark & 1) == 1 ? Util.getCurrentScript() : null;

    switch (matchedPattern) {
      case 0:
        register(SyntaxInfo.create(script, parseResult.regexes.get(0).group()));
        break;
      case 1:
        if (patterns == null) {
          Skript.error("Custom effects without inline patterns must have a patterns section.");
          return false;
        }

        patterns.forEach(subNode -> register(SyntaxInfo.create(script, subNode.getKey())));
        break;
    }

    if (matchedPattern != 1 && patterns != null) {
      Skript.error("Custom effects with inline patterns may not have a patterns section.");
      return false;
    }

    ScriptLoader.setCurrentEvent("custom effect trigger", EffectTriggerEvent.class);
    Util.getItemsFromNode(node, "trigger")
        .ifPresent(items -> whichInfo.forEach(which ->
            effectHandlers.put(which, new Trigger(ScriptLoader.currentScript.getFile(), "effect " + which, this, items))
        ));

    return true;
  }

  public static SyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }
}
