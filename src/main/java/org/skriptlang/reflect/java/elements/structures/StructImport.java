package org.skriptlang.reflect.java.elements.structures;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.command.EffectCommandEvent;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StructImport extends Structure {

	public static final Priority PRIORITY = new Priority(150);
	private static final Pattern IMPORT_STATEMENT =
			Pattern.compile("(" + SkriptMirrorUtil.PACKAGE + ")(?:\\s+as (" + SkriptMirrorUtil.IDENTIFIER + "))?");
	private static final Map<Script, Map<String, JavaType>> imports = new HashMap<>();

	static {
		Skript.registerStructure(StructImport.class, "import");
		Skript.registerEffect(EffImport.class, "import <" + IMPORT_STATEMENT.pattern() + ">");
	}

	private Script script;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, EntryContainer entryContainer) {
		this.script = getParser().getCurrentScript();
		assert entryContainer != null; // entryContainer will always be non-null as this is not a simple structure
		entryContainer.getSource().forEach(node -> registerImport(Optional.ofNullable(node.getKey())
				.map(ScriptLoader::replaceOptions)
				.orElse(null), script));
		return true;
	}

	@Override
	public boolean load() {
		return true;
	}

	@Override
	public void unload() {
		imports.remove(script);
	}

	@Override
	public Priority getPriority() {
		return PRIORITY;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "import";
	}

	/**
	 * Registers an import
	 * @param rawStatement the raw statement representing the string to import
	 * @param script the script this import belongs to
	 * @return whether the import was registered successfully
	 */
	private static boolean registerImport(String rawStatement, @Nullable Script script) {
		Matcher statement = IMPORT_STATEMENT.matcher(ScriptLoader.replaceOptions(rawStatement));
		if (!statement.matches()) {
			Skript.error(rawStatement + " is an invalid import statement.");
			return false;
		}

		String cls = statement.group(1);
		Class<?> javaClass;

		try {
			javaClass = LibraryLoader.getClassLoader().loadClass(cls);
		} catch (ClassNotFoundException ex) {
			Skript.error(cls + " refers to a non-existent class.");
			return false;
		}

		String importName = statement.group(2);

		if (javaClass.getSimpleName().equals(importName)) {
			Skript.warning(cls + " doesn't need the alias " + importName + ", as it will already be imported under that name");
		}

		if (importName == null) {
			importName = javaClass.getSimpleName();
		}

		imports.computeIfAbsent(script, s -> new HashMap<>())
			.compute(importName,
				(name, oldClass) -> {
					if (oldClass != null) {
						Skript.error(name + " is already mapped to " + oldClass.getJavaClass() + ". " +
								"It will not be remapped to " + javaClass + ".");
						return oldClass;
					}
					return new JavaType(javaClass);
				});

		return true;
	}

	public static JavaType lookup(Script script, String identifier) {
		Map<String, JavaType> localImports = imports.get(script);

		if (localImports == null)
			return null;

		return localImports.get(identifier);
	}

	public static class EffImport extends Effect {

		private String className;

		@Override
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			if (!getParser().isCurrentEvent(EffectCommandEvent.class)) {
				Skript.error("The import effect can only be used in effect commands. " +
					"To use imports in scripts, use the section.");
				return false;
			}

			className = parseResult.regexes.get(0).group();
			return registerImport(className, null); // No script in an effect command
		}

		@Override
		protected void execute(Event event) {
		}

		@Override
		public String toString(@Nullable Event e, boolean debug) {
			return "import " + className;
		}
	}

}
