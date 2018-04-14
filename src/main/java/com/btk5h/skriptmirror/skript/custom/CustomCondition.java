package com.btk5h.skriptmirror.skript.custom;


import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.*;

public class CustomCondition {
  static {
    //noinspection unchecked
    Skript.registerEvent("*Define Condition", CustomCondition.EventHandler.class,
        ConditionEvent.class,
        "condition <.+>",
        "%*classinfo% property condition <.+>");

    Skript.registerCondition(ConditionHandler.class);
    Optional<SyntaxElementInfo<? extends Condition>> info = Skript.getConditions().stream()
        .filter(i -> i.c == ConditionHandler.class)
        .findFirst();

    if (info.isPresent()) {
      thisInfo = info.get();
    } else {
      Skript.warning("Could not find custom condition class. Custom conditions will not work.");
    }
  }

  private static SyntaxElementInfo<?> thisInfo;
  private static final SectionValidator CONDITION_DECLARATION = new SectionValidator()
      .addSection("check", false);

  private static SyntaxInfo createSyntaxInfo(String pattern, boolean inverted, boolean property) {
    return new SyntaxInfo(Util.preprocessPattern(pattern), inverted, property);
  }

  private static class SyntaxInfo {
    private final String pattern;
    private final boolean inverted;
    private final boolean property;

    private SyntaxInfo(String pattern, boolean inverted, boolean property) {
      this.pattern = pattern;
      this.inverted = inverted;
      this.property = property;
    }

    public String getPattern() {
      return pattern;
    }

    public boolean isInverted() {
      return inverted;
    }

    public boolean isProperty() {
      return property;
    }

    @Override
    public String toString() {
      return String.format("%s (inverted: %s, property: %s)", pattern, inverted, property);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SyntaxInfo that = (SyntaxInfo) o;
      return inverted == that.inverted &&
          Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
      return Objects.hash(pattern, inverted);
    }
  }

  public static class ConditionEvent extends CustomSyntaxEvent {
    private final static HandlerList handlers = new HandlerList();
    private boolean markedContinue;
    private boolean markedNegated;

    public ConditionEvent(Event event, Expression<?>[] expressions,
                          SkriptParser.ParseResult parseResult) {
      super(event, expressions, parseResult);
    }

    public static HandlerList getHandlerList() {
      return handlers;
    }

    public boolean isMarkedContinue() {
      return markedContinue;
    }

    public boolean isMarkedNegated() {
      return markedNegated;
    }

    public void markContinue() {
      markedContinue = true;
    }

    public void markNegated() {
      markedNegated = true;
    }

    @Override
    public HandlerList getHandlers() {
      return handlers;
    }
  }

  private static List<String> conditions = new ArrayList<>();
  private static Map<String, SyntaxInfo> conditionInfos = new HashMap<>();
  private static Map<SyntaxInfo, Trigger> conditionHandlers = new HashMap<>();

  private static void updateConditions() {
    Util.setPatterns(thisInfo, conditions.toArray(new String[0]));
  }

  public static class EventHandler extends SelfRegisteringSkriptEvent {
    private List<SyntaxInfo> whiches = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern,
                        SkriptParser.ParseResult parseResult) {
      String c = parseResult.regexes.get(0).group();
      switch (matchedPattern) {
        case 0:
          whiches.add(createSyntaxInfo(c, false, false));
          break;
        case 1:
          String type = ((Literal<ClassInfo>) args[0]).getSingle().getCodeName();
          whiches.add(createSyntaxInfo("%" + type + "% (is|are) " + c, false, true));
          whiches.add(createSyntaxInfo("%" + type + "% (isn't|is not|aren't|are not) " + c, true, true));
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
      ScriptLoader.setCurrentEvent("custom condition check", ConditionEvent.class);
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
  }

  public static class ConditionHandler extends Condition {
    private SyntaxInfo which;
    private Expression<?>[] exprs;
    private SkriptParser.ParseResult parseResult;

    @Override
    public boolean check(Event e) {
      Trigger checker = conditionHandlers.get(which);

      if (checker == null) {
        Skript.error(
            String.format("The custom condtion '%s' no longer has a check handler.", which.getPattern())
        );
        return false;
      }

      if (which.isProperty()) {
        return checkByProperty(e, checker);
      }

      return checkByStandard(e, checker);
    }

    private boolean checkByStandard(Event e, Trigger checker) {
      ConditionEvent conditionEvent = new ConditionEvent(e, exprs, parseResult);
      checker.execute(conditionEvent);
      return conditionEvent.isMarkedContinue() ^ conditionEvent.isMarkedNegated() ^ which.isInverted();
    }

    private boolean checkByProperty(Event e, Trigger checker) {
      return exprs[0].check(e, o -> {
        Expression<?>[] localExprs = Arrays.copyOf(exprs, exprs.length);
        localExprs[0] = new SimpleLiteral<>(o, false);

        ConditionEvent conditionEvent = new ConditionEvent(e, localExprs, parseResult);
        checker.execute(conditionEvent);
        return conditionEvent.isMarkedContinue() ^ conditionEvent.isMarkedNegated();
      }, which.isInverted());
    }

    @Override
    public String toString(Event e, boolean debug) {
      return which.getPattern();
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
      String pattern = conditions.get(matchedPattern);
      which = conditionInfos.get(pattern);
      this.exprs = Arrays.stream(exprs)
          .map(Util::defendExpression)
          .toArray(Expression[]::new);
      this.parseResult = parseResult;
      return Util.canInitSafely(this.exprs);
    }
  }
}
