package com.btk5h.skriptmirror.skript.custom.condition;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.util.Utils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Name("Define Condition")
@Description({"You can find more information about this here: https://tpgamesnl.gitbook.io/skript-reflect/advanced/custom-syntax/conditions"})
public class CustomConditionSection extends CustomSyntaxSection<ConditionSyntaxInfo> {
  static {
    CustomSyntaxSection.register("Define Condition", CustomConditionSection.class,
      "[(1¦local)] condition <.+>",
      "[(1¦local)] condition",
      "[(1¦local)] %*classinfos% property condition <.+>");
  }

  private static final DataTracker<ConditionSyntaxInfo> dataTracker = new DataTracker<>();

  static Map<ConditionSyntaxInfo, Trigger> conditionHandlers = new HashMap<>();
  static Map<ConditionSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
  static Map<ConditionSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers = new HashMap<>();

  static {
    dataTracker.setSyntaxType("condition");

    Skript.registerCondition(CustomCondition.class);
    Optional<SyntaxElementInfo<? extends Condition>> info = Skript.getConditions().stream()
        .filter(i -> i.c == CustomCondition.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(conditionHandlers);
    dataTracker.addManaged(parserHandlers);
    dataTracker.addManaged(usableSuppliers);
  }

  @Override
  public DataTracker<ConditionSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         SectionNode node) {
    String what;
    SectionNode patterns = (SectionNode) node.get("patterns");
    File script = (parseResult.mark & 1) == 1 ? SkriptUtil.getCurrentScript() : null;

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

    AtomicBoolean hasCheck = new AtomicBoolean();
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

        if (key.equalsIgnoreCase("check")) {
          ScriptLoader.setCurrentEvent("custom condition check", ConditionCheckEvent.class);
          List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
          whichInfo.forEach(which -> conditionHandlers.put(which,
              new Trigger(SkriptUtil.getCurrentScript(), "condition " + which, this, items)));

          hasCheck.set(true);
          return true;
        }

        if (key.equalsIgnoreCase("parse")) {
          SyntaxParseEvent.register(this, sectionNode, whichInfo, parserHandlers);
          return true;
        }

        return false;
      });

    if (!nodesOkay)
      return false;

    if (!hasCheck.get())
      Skript.warning("Custom conditions are useless without a check section");

    return true;
  }

  public static ConditionSyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }
}
