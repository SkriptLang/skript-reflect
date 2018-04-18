package com.btk5h.skriptmirror.skript.custom.condition;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.Util;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;

import java.util.*;

public class CustomConditionSection extends SelfRegisteringSkriptEvent {
  static {
    //noinspection unchecked
    Skript.registerEvent("*Define Condition", CustomConditionSection.class, CustomSyntaxEvent.class,
        "condition <.+>",
        "%*classinfo% property condition <.+>");

    Skript.registerCondition(CustomCondition.class);
    Optional<SyntaxElementInfo<? extends Condition>> info = Skript.getConditions().stream()
        .filter(i -> i.c == CustomCondition.class)
        .findFirst();

    if (info.isPresent()) {
      CustomConditionSection.thisInfo = info.get();
    } else {
      Skript.warning("Could not find custom condition class. Custom conditions will not work.");
    }
  }

  private static SyntaxElementInfo<?> thisInfo;
  private static final SectionValidator CONDITION_DECLARATION = new SectionValidator()
      .addSection("check", false);

  static List<String> conditions = new ArrayList<>();
  static Map<String, SyntaxInfo> conditionInfos = new HashMap<>();
  static Map<SyntaxInfo, Trigger> conditionHandlers = new HashMap<>();

  private List<SyntaxInfo> whiches = new ArrayList<>();

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Literal<?>[] args, int matchedPattern,
                      SkriptParser.ParseResult parseResult) {
    String c = parseResult.regexes.get(0).group();
    switch (matchedPattern) {
      case 0:
        whiches.add(SyntaxInfo.create(c, false, false));
        break;
      case 1:
        String type = ((Literal<ClassInfo>) args[0]).getSingle().getCodeName();
        whiches.add(SyntaxInfo.create("%" + type + "% (is|are) " + c, false, true));
        whiches.add(SyntaxInfo.create("%" + type + "% (isn't|is not|aren't|are not) " + c, true, true));
        break;
    }

    whiches.forEach(which -> {
      String pattern = which.getPattern();
      if (!conditions.contains(pattern)) {
        conditions.add(pattern);
        conditionInfos.put(pattern, which);
      } else {
        Skript.error(String.format("The custom condition '%s' already has a handler.", pattern));
      }
    });

    SectionNode node = (SectionNode) SkriptLogger.getNode();
    node.convertToEntries(0);

    boolean ok = CONDITION_DECLARATION.validate(node);

    if (!ok) {
      unregister(null);
      return false;
    }

    register(node);
    return true;
  }

  @SuppressWarnings("unchecked")
  private void register(SectionNode node) {
    ScriptLoader.setCurrentEvent("custom condition check", ConditionCheckEvent.class);
    Util.getItemsFromNode(node, "check").ifPresent(items ->
        whiches.forEach(which -> conditionHandlers.put(which,
            new Trigger(ScriptLoader.currentScript.getFile(), "condition " + which, this, items)))
    );

    Util.clearSectionNode(node);
    updateConditions();
  }

  @Override
  public void register(Trigger t) {
  }

  @Override
  public void unregister(Trigger t) {
    whiches.forEach(which -> {
      conditions.remove(which.getPattern());
      conditionInfos.remove(which.getPattern());
      conditionHandlers.remove(which);
    });
    updateConditions();
  }

  @Override
  public void unregisterAll() {
    conditions.clear();
    conditionInfos.clear();
    conditionHandlers.clear();
    updateConditions();
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "conditions: " + whiches.toString();
  }

  private static void updateConditions() {
    Util.setPatterns(thisInfo, conditions.toArray(new String[0]));
  }
}
