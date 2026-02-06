package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.patterns.PatternCompiler;
import ch.njol.skript.patterns.TypePatternElement;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.SkriptMirror;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class CustomSyntaxStructure<C extends CustomSyntax<?>> extends Structure {

	public static void register() {
		ParserInstance.registerData(ExpressionsData.class, ExpressionsData::new);
	}

	public static final Priority PRIORITY = new Priority(350);

	protected EntryContainer entryContainer;
	protected boolean local;
	protected String[] patterns;
	protected boolean hasPatternsSection, hasParseSection;

	protected C customSyntax;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {
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
		getParser().getData(ExpressionsData.class).computePossibleReturnTypes(patterns);
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

	public static class ExpressionsData extends ParserInstance.Data {

		private final List<Set<Class<?>>> possibleReturnTypes;
		private final List<Kleenean> plurals;

		public ExpressionsData(ParserInstance parserInstance) {
			super(parserInstance);
			this.possibleReturnTypes = new ArrayList<>();
			this.plurals = new ArrayList<>();
		}

		private void computePossibleReturnTypes(String[] patterns) {
			for (String pattern : patterns) {
				List<TypePatternElement> elements = PatternCompiler.compile(pattern).getElements(TypePatternElement.class);
				for (int expressionIndex = 0; expressionIndex < elements.size(); expressionIndex++) {
					TypePatternElement element = elements.get(expressionIndex);
					SkriptParser.ExprInfo info = element.getExprInfo();

					while (possibleReturnTypes.size() <= expressionIndex) {
						possibleReturnTypes.add(new HashSet<>());
						plurals.add(null);
					}
					Set<Class<?>> set = possibleReturnTypes.get(expressionIndex);

					for (int j = 0; j < info.classes.length; j++) {
						Class<?> type = info.classes[j].getC();
						if (!set.add(type))
							continue;
						plurals.set(expressionIndex, updatePlurality(plurals.get(expressionIndex), type.isArray()));
					}
				}
			}
		}

		public static Kleenean updatePlurality(@Nullable Kleenean current, boolean plural) {
			if (current == null)
				return Kleenean.get(plural);

			if (current.isTrue() != plural)
				return Kleenean.UNKNOWN;

			return current;
		}

		public Class<?>[] possibleReturnTypes(int expressionIndex) {
			return possibleReturnTypes.get(expressionIndex).toArray(new Class[0]);
		}

		public boolean testPlurality(int expressionIndex, boolean plural) {
			return switch (plurals.get(expressionIndex)) {
				case UNKNOWN -> true;
				case TRUE -> plural;
				case FALSE -> !plural;
			};
		}

	}

}
