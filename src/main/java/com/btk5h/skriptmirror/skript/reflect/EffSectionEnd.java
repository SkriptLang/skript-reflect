package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

// Internal use only
public class EffSectionEnd extends Effect {

  static {
    Skript.registerEffect(EffSectionEnd.class, "$ end skriptmirror section");
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    return true;
  }

  @Override
  protected TriggerItem walk(final Event e) {
    TriggerSection parent = this.getParent();
    if (parent instanceof Conditional) {
      Condition condition = SkriptReflection.getCondition((Conditional) parent);
      if (condition instanceof CondSection && !((CondSection) condition).shouldContinue(e, getNext()))
        return null;
    }

    return super.walk(e);
  }

  @Override
  protected void execute(Event e) {
    throw new IllegalStateException();
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return "$ end skriptmirror section";
  }

}
