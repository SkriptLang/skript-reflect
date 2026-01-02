package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import com.btk5h.skriptmirror.SkriptMirror;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

public abstract class CustomSyntaxStructure<C extends CustomSyntax<?>> extends Structure {

	public static final Priority PRIORITY = new Priority(350);

	protected EntryContainer entryContainer;
	protected boolean local;
	protected String[] patterns;
	protected boolean hasPatternsSection, hasParseSection;

	protected C customSyntax;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {
		this.entryContainer = entryContainer;
		this.local = parseResult.hasTag("local");
		this.hasParseSection = entryContainer.hasEntry("parse");
		this.hasPatternsSection = matchedPattern == 1;
		if (hasPatternsSection && !parseResult.regexes.isEmpty()) {
			Skript.error("Cannot use both an inline pattern and a 'patterns' entry at the same time.");
			return false;
		}
		this.patterns = hasPatternsSection
			? entryContainer.get("patterns", String[].class, false)
			: new String[] {parseResult.regexes.get(0).group()};
		return true;
	}

	@Override
	public boolean preLoad() {
		customSyntax = createCustomSyntax();
		return customSyntax.register(SkriptMirror.getAddonInstance().syntaxRegistry());
	}

	@Override
	public boolean load() {
		ParserInstance parser = getParser();
		if (hasParseSection) {
			parser.setCurrentEvent("custom syntax parser", SyntaxParseEvent.class);
			customSyntax.parseTrigger(entryContainer.get("parse", Trigger.class, true));
			parser.deleteCurrentEvent();
		}
		return true;
	}

	@Override
	public void unload() {
		customSyntax.unregister(SkriptMirror.getAddonInstance().syntaxRegistry());
	}

	protected abstract C createCustomSyntax();

	@Override
	public Priority getPriority() {
		return PRIORITY;
	}

}
