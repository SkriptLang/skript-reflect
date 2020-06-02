package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class CustomEvent extends SkriptEvent {

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
    return null;
  }

}
