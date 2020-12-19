package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;

public interface PreloadableEvent {

  boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, boolean isPreload);

}
