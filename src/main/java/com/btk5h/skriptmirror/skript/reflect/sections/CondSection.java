package com.btk5h.skriptmirror.skript.reflect.sections;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Create Section")
@Description({"You can find more information about this here: https://tpgamesnl.gitbook.io/skript-reflect/advanced/reflection/sections#creating-a-section"})
public class CondSection extends Condition {

  static {
    Skript.registerCondition(CondSection.class,
      "create [new] section [with [arguments variables] %-objects%] (and store it|stored) in %objects%");
  }

  private boolean shouldInit = true;
  private Trigger trigger;
  private ArrayList<Variable<?>> variableArguments;
  private Expression<Object> variableStore;

  @SuppressWarnings("ConstantConditions")
  public CondSection() {
    Node node = SkriptLogger.getNode();
    if (!(node instanceof SectionNode)) {
      shouldInit = false;
      return;
    }

    SectionNode sectionNode = (SectionNode) node;
    ArrayList<Node> nodes = SkriptReflection.getNodes(sectionNode);

    SectionNode newSectionNode = new SectionNode(sectionNode.getKey(), "", sectionNode.getParent(),
      sectionNode.getLine());
    SkriptReflection.getNodes(newSectionNode).addAll(nodes);
    nodes.clear();
    Class<? extends Event>[] currentEvents = ScriptLoader.getCurrentEvents();
    String currentEventName = ScriptLoader.getCurrentEventName();
    ScriptLoader.setCurrentEvent("section event", SectionEvent.class);
    List<TriggerItem> triggerItemList = SkriptUtil.getItemsFromNode(newSectionNode);
    ScriptLoader.setCurrentEvent(currentEventName, currentEvents);
    trigger = new Trigger(SkriptUtil.getCurrentScript(), "section", new SkriptEvent() {
      @Override
      public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return false;
      }

      @Override
      public boolean check(Event e) {
        return false;
      }

      @Override
      public String toString(@Nullable Event e, boolean debug) {
        return "section";
      }
    }, triggerItemList);
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    if (!shouldInit)
      return false;

    variableArguments = new ArrayList<>();
    Expression<?> varList = SkriptUtil.defendExpression(exprs[0]);
    if (varList != null) {
      if (varList instanceof ExpressionList) {
        ExpressionList<?> expressionList = (ExpressionList<?>) varList;

        for (Expression<?> expr : expressionList.getExpressions()) {
          if (!(expr instanceof Variable)) {
            Skript.error("The arguments can only contain variables");
            return false;
          }

          variableArguments.add((Variable<?>) expr);
        }
      } else if (varList instanceof Variable) {
        variableArguments.add((Variable<?>) varList);
      } else {
        Skript.error("The arguments can only contain variables");
        return false;
      }
    }

    variableStore = SkriptUtil.defendExpression(exprs[1]);
    if (!(variableStore instanceof Variable)) {
      Skript.error("The created section can only be stored in a variable");
      return false;
    }

    return SkriptUtil.canInitSafely(variableStore);
  }

  @Override
  public boolean check(Event e) {
    Section section = new Section(trigger, e, variableArguments);
    variableStore.change(e, new Section[]{section}, Changer.ChangeMode.SET);
    return false;
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return "new section";
  }

}
