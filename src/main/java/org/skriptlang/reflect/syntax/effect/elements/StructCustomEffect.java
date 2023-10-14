package org.skriptlang.reflect.syntax.effect.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.skriptlang.reflect.syntax.effect.EffectSyntaxInfo;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class StructCustomEffect extends CustomSyntaxStructure<EffectSyntaxInfo> {

  public static boolean customEffectsUsed = false;

  static {
    String[] syntax = {
      "[(1¦local)] effect <.+>",
      "[(1¦local)] effect"
    };
    Skript.registerStructure(StructCustomEffect.class, customSyntaxValidator()
        .addSection("trigger", true)
        .build(), syntax);
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

  private SectionNode parseNode, triggerNode;

  @Override
  public DataTracker<EffectSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         EntryContainer entryContainer) {
    customEffectsUsed = true;

    SectionNode patterns = entryContainer.getOptional("patterns", SectionNode.class, false);
    Script script = (parseResult.mark & 1) == 1 ? SkriptUtil.getCurrentScript() : null;

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

    return true;
  }

  @Override
  public boolean preLoad() {
    super.preLoad();

    EntryContainer entryContainer = getEntryContainer();
    parseNode = entryContainer.getOptional("parse", SectionNode.class, false);
    SectionNode safeParseNode = entryContainer.getOptional("safe parse", SectionNode.class, false);
    if (parseNode != null) {
      if (safeParseNode != null) {
        Skript.error("You can't have two parse sections");
        return false;
      }
      whichInfo.forEach(which -> parseSectionLoaded.put(which, false));
    } else {
      if (safeParseNode != null) {
        SyntaxParseEvent.register(safeParseNode, whichInfo, parserHandlers);

        whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
      }
    }

    SectionNode usableInNode = entryContainer.getOptional("usable in", SectionNode.class, false);
    if (usableInNode != null && !handleUsableSection(usableInNode, usableSuppliers))
      return false;

    triggerNode = entryContainer.getOptional("trigger", SectionNode.class, false);
    if (triggerNode == null)
      Skript.warning("Custom effects are useless without a trigger section");

    return true;
  }

  @Override
  public boolean load() {
    if (parseNode != null) {
      SkriptLogger.setNode(parseNode);
      SyntaxParseEvent.register(parseNode, whichInfo, parserHandlers);

      whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
    }

    if (triggerNode != null) {
      SkriptLogger.setNode(triggerNode);
      getParser().setCurrentEvent("custom effect trigger", EffectTriggerEvent.class);
      List<TriggerItem> items = SkriptUtil.getItemsFromNode(triggerNode);
      whichInfo.forEach(which ->
          effectHandlers.put(which,
              new Trigger(getParser().getCurrentScript(), "effect " + which, new SimpleEvent(), items)));
    }
    SkriptLogger.setNode(null);

    return true;
  }

  public static EffectSyntaxInfo lookup(Script script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }

}
