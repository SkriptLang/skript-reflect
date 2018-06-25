package com.btk5h.skriptmirror.skript.custom.condition;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CustomConditionSection extends CustomSyntaxSection<ConditionSyntaxInfo> {
  static {
    //noinspection unchecked
    CustomSyntaxSection.register("Define Condition", CustomConditionSection.class,
        "[(1¦local)] condition <.+>",
        "[(1¦local)] condition",
        "[(1¦local)] %*classinfo% property condition <.+>");
  }

  private static DataTracker<ConditionSyntaxInfo> dataTracker = new DataTracker<>();

  static Map<ConditionSyntaxInfo, Trigger> conditionHandlers = new HashMap<>();
  static Map<ConditionSyntaxInfo, Trigger> parserHandlers = new HashMap<>();

  static {
    dataTracker.setSyntaxType("condition");

    dataTracker.getValidator()
        .addSection("check", false)
        .addSection("patterns", true)
        .addSection("parse", true);

    Skript.registerCondition(CustomCondition.class);
    Optional<SyntaxElementInfo<? extends Condition>> info = Skript.getConditions().stream()
        .filter(i -> i.c == CustomCondition.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(conditionHandlers);
    dataTracker.addManaged(parserHandlers);
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
        String type = ((Literal<ClassInfo>) args[0]).getSingle().getCodeName();
        register(ConditionSyntaxInfo.create(script, "%" + type + "% (is|are) " + what, 1, false, true));
        register(
            ConditionSyntaxInfo.create(script, "%" + type + "% (isn't|is not|aren't|are not) " + what, 1, true, true));
        break;
    }

    if (matchedPattern != 1 && patterns != null) {
      Skript.error("Custom conditions with inline patterns may not have a patterns section.");
      return false;
    }

    ScriptLoader.setCurrentEvent("custom condition check", ConditionCheckEvent.class);
    SkriptUtil.getItemsFromNode(node, "check").ifPresent(items ->
        whichInfo.forEach(which -> conditionHandlers.put(which,
            new Trigger(ScriptLoader.currentScript.getFile(), "condition " + which, this, items)))
    );

    SyntaxParseEvent.register(this, node, whichInfo, parserHandlers);

    return true;
  }

  public static ConditionSyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }
}
