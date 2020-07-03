package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

// TODO documentation
// TODO maybe not use bukkit scheduler because delays (2 ticks for async section)

public class SectionAsync extends CondSection {

  private Trigger trigger;
  private boolean isSync;
  private Object localVariables;

  // TODO Disabled for now because of too many bugs
  // https://pastebin.com/3gbBgBrF
//  static {
//    register(SectionAsync.class,
//      "(async|await)",
//      "sync");
//  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    isSync = matchedPattern == 1;
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
    localVariables = SkriptReflection.getLocals(e);
  }

  @Override
  public boolean shouldContinue(Event e, TriggerItem next) {
    boolean startedAsync = !Bukkit.isPrimaryThread();

    // fix 12354 by appending continue code to trigger
    // TriggerSection#setNext
    // or add `go (sync|async)` effect, and use that instead of this whole thing, and just add sections

    Runnable runnable = () -> {
      SkriptReflection.copyVariablesMapFromMap(localVariables, e);
      Bukkit.broadcastMessage("[" + hashCode() + "] Running provided code");
      TriggerItem.walk(trigger, e);
      Bukkit.broadcastMessage("[" + hashCode() + "] Provided code over");

      localVariables = SkriptReflection.getLocals(e);

      Runnable continuation = () -> {
        if (next != null) {
          SkriptReflection.copyVariablesMapFromMap(localVariables, e);
          Bukkit.broadcastMessage("[" + hashCode() + "] Continuing");
          TriggerItem.walk(next, e);
          Bukkit.broadcastMessage("[" + hashCode() + "] Done continuing");
        }
        SkriptReflection.removeLocals(e);
      };

      if (startedAsync)
        Bukkit.getScheduler().runTaskAsynchronously(SkriptMirror.getInstance(), continuation);
      else
        Bukkit.getScheduler().runTask(SkriptMirror.getInstance(), continuation);
    };

    if (isSync)
      Bukkit.getScheduler().runTask(SkriptMirror.getInstance(), runnable);
    else
      Bukkit.getScheduler().runTaskAsynchronously(SkriptMirror.getInstance(), runnable);

    return false;
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return "await";
  }

}
