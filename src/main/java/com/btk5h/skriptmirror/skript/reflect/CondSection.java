package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;

public abstract class CondSection extends Condition {

  protected EffSectionEnd effSectionEnd;

  @SuppressWarnings("ConstantConditions")
  public CondSection() {
    Node node = SkriptLogger.getNode();
    if (!(node instanceof SectionNode))
      return;
    SectionNode sectionNode = (SectionNode) node;
    ArrayList<Node> nodes = SkriptReflection.getNodes(sectionNode);

    SectionNode newSectionNode = new SectionNode(sectionNode.getKey(), "", sectionNode.getParent(),
      sectionNode.getLine());
    SkriptReflection.getNodes(newSectionNode).addAll(nodes);
    handleSectionNode(newSectionNode);

    nodes.clear();
    nodes.add(new SimpleNode("$ end skriptmirror section", "", -1, sectionNode.getParent()));
  }

  protected static void register(Class<? extends CondSection> clazz, String... patterns) {
    Skript.registerCondition(clazz, patterns);
  }

  // todo line endings LF -> CRLF

  void setEffSectionEnd(EffSectionEnd effSectionEnd) {
    this.effSectionEnd = effSectionEnd;
  }

  @Override
  public abstract boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult);

  protected abstract void handleSectionNode(SectionNode sectionNode);

  @Override
  public boolean check(Event e) {
    execute(e);
    return true;
  }

  protected abstract void execute(Event e);

  public abstract boolean shouldContinue(Event e, TriggerItem next);

  @Override
  public abstract String toString(@Nullable Event e, boolean debug);

}
