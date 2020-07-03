package com.btk5h.skriptmirror.skript.reflect.trycatch;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.reflect.CondSection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

// TODO maybe throw & catch NPE's when java call on null

public class SectionTry extends CondSection {

  public static SectionTry sectionTry = null;
  private Trigger trigger;
  private Throwable throwable;

  // TODO Disabled for now because it's not done yet
//  static {
//    register(SectionTry.class, "try");
//  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    return true;
  }

  @Override
  public void handleSectionNode(SectionNode sectionNode) {
    List<TriggerItem> triggerItemList = SkriptUtil.getItemsFromNode(sectionNode);

    trigger = new Trigger(SkriptUtil.getCurrentScript(), "section", new SkriptEvent() {
      @Override
      public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return false;
      }

      @Override
      public boolean check(Event e) {
        return true;
      }

      @Override
      public String toString(@Nullable Event e, boolean debug) {
        return null;
      }
    }, triggerItemList);
  }

  @Override
  public void execute(Event e) {
    sectionTry = this;
    TriggerItem.walk(trigger, e);
    sectionTry = null;

    if (throwable != null) {
      Bukkit.getLogger().info("caught " + throwable);
    }
  }

  @Override
  public boolean shouldContinue(Event e, TriggerItem next) {
    return true;
  }

  public void setThrowable(Throwable throwable) {
    this.throwable = throwable;
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return null;
  }
}
