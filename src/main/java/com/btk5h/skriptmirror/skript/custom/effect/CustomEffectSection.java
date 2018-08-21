package com.btk5h.skriptmirror.skript.custom.effect;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CustomEffectSection extends CustomSyntaxSection<EffectSyntaxInfo> {
  static {
    //noinspection unchecked
    CustomSyntaxSection.register("Define Effect", CustomEffectSection.class,
        "[(1¦local)] effect <.+>",
        "[(1¦local)] effect");
  }

  private static DataTracker<EffectSyntaxInfo> dataTracker = new DataTracker<>();

  static final Map<EffectSyntaxInfo, Trigger> effectHandlers = new HashMap<>();
  static final Map<EffectSyntaxInfo, Trigger> parserHandlers = new HashMap<>();

  static {
    dataTracker.setSyntaxType("effect");

    Skript.registerEffect(CustomEffect.class);
    Optional<SyntaxElementInfo<? extends Effect>> info = Skript.getEffects().stream()
        .filter(i -> i.c == CustomEffect.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(effectHandlers);
    dataTracker.addManaged(parserHandlers);
  }

  @Override
  public DataTracker<EffectSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         SectionNode node) {
    SectionNode patterns = (SectionNode) node.get("patterns");
    File script = (parseResult.mark & 1) == 1 ? SkriptUtil.getCurrentScript() : null;

    switch (matchedPattern) {
      case 0:
        register(EffectSyntaxInfo.create(script, parseResult.regexes.get(0).group(), 1));
        break;
      case 1:
        if (patterns == null) {
          Skript.error("Custom effects without inline patterns must have a patterns section.");
          return false;
        }

        int i = 1;
        for (Node subNode : patterns) {
          register(EffectSyntaxInfo.create(script, subNode.getKey(), i++));
        }
        break;
    }

    if (matchedPattern != 1 && patterns != null) {
      Skript.error("Custom effects with inline patterns may not have a patterns section.");
      return false;
    }

    return handleEntriesAndSections(node,
        entryNode -> false,
        sectionNode -> {
          String key = sectionNode.getKey();

          if (key.equalsIgnoreCase("patterns")) {
            return true;
          }

          if (key.equalsIgnoreCase("trigger")) {
            ScriptLoader.setCurrentEvent("custom effect trigger", EffectTriggerEvent.class);
            List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
            whichInfo.forEach(which ->
                effectHandlers.put(which,
                    new Trigger(ScriptLoader.currentScript.getFile(), "effect " + which, this, items)));
            return true;
          }

          if (key.equalsIgnoreCase("parse")) {
            SyntaxParseEvent.register(this, sectionNode, whichInfo, parserHandlers);
            return true;
          }

          return false;
        });
  }

  public static EffectSyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }
}
