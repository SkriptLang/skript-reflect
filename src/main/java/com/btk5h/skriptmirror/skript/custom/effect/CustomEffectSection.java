package com.btk5h.skriptmirror.skript.custom.effect;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.skript.custom.PreloadListener;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class CustomEffectSection extends CustomSyntaxSection<EffectSyntaxInfo> {

  public static boolean customEffectsUsed = false;

  static {
    String[] syntax = {
      "[(1¦local)] effect <.+>",
      "[(1¦local)] effect"
    };
    CustomSyntaxSection.register("Define Effect", CustomEffectSection.class, syntax);
    PreloadListener.addSyntax(CustomEffectSection.class, syntax);
  }

  private static final DataTracker<EffectSyntaxInfo> dataTracker = new DataTracker<>();

  static final Map<EffectSyntaxInfo, Trigger> effectHandlers = new HashMap<>();
  static final Map<EffectSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
  static final Map<EffectSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers = new HashMap<>();
  static final Map<EffectSyntaxInfo, Boolean> parseSectionLoaded = new HashMap<>();

  static {
    Skript.registerEffect(CustomEffect.class);
    Optional<SyntaxElementInfo<? extends Effect>> info = Skript.getEffects().stream()
      .filter(i -> i.c == CustomEffect.class)
      .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(effectHandlers);
    dataTracker.addManaged(parserHandlers);
    dataTracker.addManaged(usableSuppliers);
    dataTracker.addManaged(parseSectionLoaded);
  }

  @Override
  public DataTracker<EffectSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         SectionNode node, boolean isPreload) {
    customEffectsUsed = true;

    if (!isPreloaded) {
      SectionNode patterns = (SectionNode) node.get("patterns");
      File script = (parseResult.mark & 1) == 1 ? SkriptUtil.getCurrentScriptFile() : null;

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

      if (node.get("parse") != null) {
        if (node.get("safe parse") != null) {
          Skript.error("You can't have two parse sections");
          return false;
        }
        whichInfo.forEach(which -> parseSectionLoaded.put(which, false));
      } else {
        SectionNode safeParseNode = (SectionNode) node.get("safe parse");
        if (safeParseNode != null) {
          SyntaxParseEvent.register(this, safeParseNode, whichInfo, parserHandlers);

          whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
        }
      }
    }

    AtomicBoolean hasTrigger = new AtomicBoolean();
    boolean nodesOkay = handleEntriesAndSections(node,
      entryNode -> false,
      sectionNode -> {
        String key = sectionNode.getKey();
        assert key != null;

        if (key.equalsIgnoreCase("patterns")) {
          return true;
        }

        if (key.equalsIgnoreCase("usable in")) {
          return handleUsableSection(sectionNode, usableSuppliers);
        }

        if (key.equalsIgnoreCase("trigger")) {
          hasTrigger.set(true);
          return true;
        }

        if (key.equalsIgnoreCase("parse"))
          return true;

        if (key.equalsIgnoreCase("safe parse"))
          return true;

        return false;
      });

    if (!nodesOkay)
      return false;

    if (!hasTrigger.get())
      Skript.warning("Custom effects are useless without a trigger section");

    if (!isPreload) {
      SectionNode sectionNode = (SectionNode) node.get("parse");
      if (sectionNode != null) {
        SkriptLogger.setNode(sectionNode);
        SyntaxParseEvent.register(this, sectionNode, whichInfo, parserHandlers);

        whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
      }

      sectionNode = (SectionNode) node.get("trigger");
      if (sectionNode != null) {
        SkriptLogger.setNode(sectionNode);
        getParser().setCurrentEvent("custom effect trigger", EffectTriggerEvent.class);
        List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
        whichInfo.forEach(which ->
          effectHandlers.put(which,
            new Trigger(SkriptUtil.getCurrentScript(), "effect " + which, this, items)));
      }
      SkriptLogger.setNode(null);
    }

    return true;
  }

  public static EffectSyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }
}
