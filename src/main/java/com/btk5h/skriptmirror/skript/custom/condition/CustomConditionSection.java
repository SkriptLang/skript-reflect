package com.btk5h.skriptmirror.skript.custom.condition;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import com.btk5h.skriptmirror.Util;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CustomConditionSection extends CustomSyntaxSection<SyntaxInfo> {
  static {
    //noinspection unchecked
    CustomSyntaxSection.register("Define Condition", CustomConditionSection.class,
        "condition <.+>",
        "condition",
        "%*classinfo% property condition <.+>");
  }

  private static DataTracker<SyntaxInfo> dataTracker = new DataTracker<>();

  static Map<SyntaxInfo, Trigger> conditionHandlers = new HashMap<>();

  static {
    dataTracker.setSyntaxType("condition");

    dataTracker.getValidator()
        .addSection("check", false);

    Skript.registerCondition(CustomCondition.class);
    Optional<SyntaxElementInfo<? extends Condition>> info = Skript.getConditions().stream()
        .filter(i -> i.c == CustomCondition.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(conditionHandlers);
  }

  @Override
  public DataTracker<SyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         SectionNode node) {
    String what;
    SectionNode patterns = (SectionNode) node.get("patterns");

    switch (matchedPattern) {
      case 0:
        what = parseResult.regexes.get(0).group();
        register(SyntaxInfo.create(what, false, false));
        break;
      case 1:
        if (patterns == null) {
          Skript.error("Custom conditions without inline patterns must have a patterns section.");
          return false;
        }

        patterns.forEach(subNode -> register(SyntaxInfo.create(subNode.getKey(), false, false)));
        break;
      case 2:
        what = parseResult.regexes.get(0).group();
        String type = ((Literal<ClassInfo>) args[0]).getSingle().getCodeName();
        register(SyntaxInfo.create("%" + type + "% (is|are) " + what, false, true));
        register(SyntaxInfo.create("%" + type + "% (isn't|is not|aren't|are not) " + what, true, true));
        break;
    }

    if (matchedPattern != 1 && patterns != null) {
      Skript.error("Custom conditions with inline patterns may not have a patterns section.");
      return false;
    }

    ScriptLoader.setCurrentEvent("custom condition check", ConditionCheckEvent.class);
    Util.getItemsFromNode(node, "check").ifPresent(items ->
        whichInfo.forEach(which -> conditionHandlers.put(which,
            new Trigger(ScriptLoader.currentScript.getFile(), "condition " + which, this, items)))
    );

    return true;
  }

  public static SyntaxInfo lookup(int matchedPattern) {
    return dataTracker.lookup(matchedPattern);
  }
}
