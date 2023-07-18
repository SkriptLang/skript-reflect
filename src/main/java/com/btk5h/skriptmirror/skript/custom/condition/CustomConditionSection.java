package com.btk5h.skriptmirror.skript.custom.condition;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Utils;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.skript.custom.PreloadListener;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CustomConditionSection extends CustomSyntaxSection<ConditionSyntaxInfo> {

  public static boolean customConditionsUsed = false;

  static {
    String[] syntax = {
      "[(1¦local)] condition <.+>",
      "[(1¦local)] condition",
      "[(1¦local)] %*classinfos% property condition <.+>"
    };
    CustomSyntaxSection.register("Define Condition", CustomConditionSection.class, syntax);
    PreloadListener.addSyntax(CustomConditionSection.class, syntax);
  }

  private static final DataTracker<ConditionSyntaxInfo> dataTracker = new DataTracker<>();

  static final Map<ConditionSyntaxInfo, Trigger> conditionHandlers = new HashMap<>();
  static final Map<ConditionSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
  static final Map<ConditionSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers = new HashMap<>();
  static final Map<ConditionSyntaxInfo, Boolean> parseSectionLoaded = new HashMap<>();

  static {
    Skript.registerCondition(CustomCondition.class);
    Optional<SyntaxElementInfo<? extends Condition>> info = Skript.getConditions().stream()
        .filter(i -> i.c == CustomCondition.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(conditionHandlers);
    dataTracker.addManaged(parserHandlers);
    dataTracker.addManaged(usableSuppliers);
    dataTracker.addManaged(parseSectionLoaded);
  }

  @Override
  public DataTracker<ConditionSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         SectionNode node, boolean isPreload) {
    customConditionsUsed = true;

    if (!isPreloaded) {
      String what;
      SectionNode patterns = (SectionNode) node.get("patterns");
      File script = (parseResult.mark & 1) == 1 ? SkriptUtil.getCurrentScriptFile() : null;

      switch (matchedPattern) {
        case 0:
          what = parseResult.regexes.get(0).group();
          register(ConditionSyntaxInfo.create(script, what, 1, false, false));
          break;
        case 1:
          if (patterns == null) {
            Skript.error("Custom conditions without inline patterns must have a patterns section.");
            return false;
          }

          int i = 1;
          for (Node subNode : patterns) {
            register(ConditionSyntaxInfo.create(script, subNode.getKey(), i++, false, false));
          }
          break;
        case 2:
          what = parseResult.regexes.get(0).group();
          String type = Arrays.stream(((Literal<ClassInfo>) args[0]).getArray())
            .map(ClassInfo::getCodeName)
            .map(codeName -> {
              boolean isPlural = Utils.getEnglishPlural(codeName).getSecond();

              if (!isPlural) {
                return Utils.toEnglishPlural(codeName);
              }

              return codeName;
            })
            .collect(Collectors.joining("/"));
          register(ConditionSyntaxInfo.create(script, "%" + type + "% (is|are) " + what, 1, false, true));
          register(
            ConditionSyntaxInfo.create(script, "%" + type + "% (isn't|is not|aren't|are not) " + what, 1, true, true));
          break;
      }

      if (matchedPattern != 1 && patterns != null) {
        Skript.error("Custom conditions with inline patterns may not have a patterns section.");
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

    AtomicBoolean hasCheck = new AtomicBoolean();
    boolean nodesOkay = handleEntriesAndSections(node,
      entryNode -> false,
      sectionNode -> {
        String key = sectionNode.getKey();
        assert key != null;

        if (key.equalsIgnoreCase("patterns"))
          return true;

        if (key.equalsIgnoreCase("usable in"))
          return handleUsableSection(sectionNode, usableSuppliers);

        if (key.equalsIgnoreCase("check")) {
          hasCheck.set(true);

          return true;
        }

        if (key.equalsIgnoreCase("parse") || key.equalsIgnoreCase("safe parse"))
          return true;

        return false;
      });

    if (!nodesOkay)
      return false;

    if (!hasCheck.get())
      Skript.warning("Custom conditions are useless without a check section");

    if (!isPreload) {
      SectionNode sectionNode = (SectionNode) node.get("parse");
      if (sectionNode != null) {
        SkriptLogger.setNode(sectionNode);
        SyntaxParseEvent.register(this, sectionNode, whichInfo, parserHandlers);

        whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
      }

      sectionNode = (SectionNode) node.get("check");
      if (sectionNode != null) {
        SkriptLogger.setNode(sectionNode);
        getParser().setCurrentEvent("custom condition check", ConditionCheckEvent.class);
        List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
        whichInfo.forEach(which -> conditionHandlers.put(which,
          new Trigger(SkriptUtil.getCurrentScriptFile(), "condition " + which, this, items)));
      }
      SkriptLogger.setNode(null);
    }

    return true;
  }

  public static ConditionSyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }
}
