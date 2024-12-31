package org.skriptlang.reflect.syntax;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.VariableString;
import com.btk5h.skriptmirror.SkriptMirror;
import org.skriptlang.reflect.java.elements.structures.StructImport;
import org.skriptlang.reflect.syntax.event.elements.CustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import com.btk5h.skriptmirror.JavaType;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.elements.CustomEventUtils;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class CustomSyntaxStructure<T extends CustomSyntaxStructure.SyntaxData> extends Structure {

	public static final String DEFAULT_PATTERN = "this is here because at least one pattern is required";
	public static final Priority PRIORITY = new Priority(350);

	public static class CustomSyntaxEvent extends Event {
		private CustomSyntaxEvent() {}

		@Override
		public HandlerList getHandlers() {
			throw new IllegalStateException();
		}
	}

	public static class DataTracker<T> {
		public DataTracker() {
		}

		private List<String> patterns = new ArrayList<>();
		private final Map<Script, Map<String, T>> primaryData = new HashMap<>();
		private final List<Map<T, ?>> managedData = new ArrayList<>();
		private SyntaxRegistry.Key<?> syntaxKey;
		private SyntaxInfo<?> info;

		public List<String> getPatterns() {
			return patterns;
		}

		public Map<Script, Map<String, T>> getPrimaryData() {
			return primaryData;
		}

		public List<Map<T, ?>> getManagedData() {
			return managedData;
		}

		public SyntaxInfo<?> getInfo() {
			return info;
		}

		public void recomputePatterns() {
			patterns = primaryData.values().stream()
					.map(Map::keySet)
					.flatMap(Set::stream)
					.distinct()
					.collect(Collectors.toList());
			patterns.add(0, DEFAULT_PATTERN); // registration api compatibility workaround
		}

		public void addManaged(Map<T, ?> data) {
			managedData.add(data);
		}

		public void setInfo(SyntaxInfo<?> info) {
			this.info = info;
		}

		public final T lookup(Script script, int matchedPattern) {
			String originalSyntax = patterns.get(matchedPattern);
			Map<String, T> localSyntax = primaryData.get(script);

			if (localSyntax != null) {
				T privateResult = localSyntax.get(originalSyntax);
				if (privateResult != null) {
					return privateResult;
				}
			}

			Map<String, T> globalSyntax = primaryData.get(null);

			if (globalSyntax == null || !globalSyntax.containsKey(originalSyntax)) {
				return null;
			}

			return globalSyntax.get(originalSyntax);
		}

		public SyntaxRegistry.Key<?> getSyntaxKey() {
			return syntaxKey;
		}

		public void setSyntaxKey(SyntaxRegistry.Key<?> syntaxKey) {
			this.syntaxKey = syntaxKey;
		}
	}

	public abstract static class SyntaxData {
		private final Script script;
		private final String pattern;
		private final int matchedPattern;

		protected SyntaxData(Script script, String pattern, int matchedPattern) {
			this.script = script;
			this.pattern = pattern;
			this.matchedPattern = matchedPattern;
		}

		public Script getScript() {
			return script;
		}

		public String getPattern() {
			return pattern;
		}

		public int getMatchedPattern() {
			return matchedPattern;
		}

		@Override
		public String toString() {
			return pattern;
		}
	}

	protected List<T> whichInfo = new ArrayList<>();
	private boolean hasPatterns = false;

	protected abstract DataTracker<T> getDataTracker();

	@Override
	public boolean preLoad() {
		update();
		return true;
	}

	@Override
	public boolean load() {
		return true;
	}

	@Override
	public void unload() {
		whichInfo.forEach(which -> {
			Map<Script, Map<String, T>> primaryData = getDataTracker().getPrimaryData();
			Script script = which.getScript();

			Map<String, T> syntaxes = primaryData.get(script);
			syntaxes.remove(which.getPattern());
			if (syntaxes.isEmpty()) {
				primaryData.remove(script);
			}

			getDataTracker().getManagedData().forEach(data -> data.remove(which));
		});

		update();
	}

	@Override
	public Priority getPriority() {
		return PRIORITY;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return null;
	}

	private void update() {
		getDataTracker().recomputePatterns();
		SyntaxRegistry syntaxRegistry = SkriptMirror.getAddonInstance().syntaxRegistry();
		SyntaxInfo<?> oldSyntaxInfo = getDataTracker().getInfo();
		// an angel weeps
		syntaxRegistry.unregister((SyntaxRegistry.Key) getDataTracker().getSyntaxKey(), (SyntaxInfo<?>) oldSyntaxInfo);
		SyntaxInfo<?> newSyntaxInfo = oldSyntaxInfo.toBuilder()
				.clearPatterns()
				.addPatterns(getDataTracker().getPatterns())
				.build();
		syntaxRegistry.register((SyntaxRegistry.Key) getDataTracker().getSyntaxKey(), newSyntaxInfo);
		getDataTracker().setInfo(newSyntaxInfo);
	}

	protected final void register(T data) {
		hasPatterns = true;
		String pattern = data.getPattern();

		whichInfo.add(data);

		getDataTracker().getPrimaryData()
			.computeIfAbsent(data.getScript(), f -> new HashMap<>())
			.put(pattern, data);
	}

	protected boolean checkHasPatterns() {
		if (hasPatterns)
			return true;
		Skript.error("A custom syntax must have at least one pattern");
		return false;
	}

	protected SectionNode[] getParseNode() {
		SectionNode parseNode = getEntryContainer().getOptional("parse", SectionNode.class, false);
		SectionNode safeParseNode = getEntryContainer().getOptional("safe parse", SectionNode.class, false);
		if (safeParseNode != null) {
			if (parseNode != null) {
				Skript.error("A custom syntax element cannot contain both 'parse' and 'safe parse' entries");
				return null;
			}
			Skript.warning("The 'safe parse' entry is deprecated and will act as a regular 'parse' entry."
				+ " Please use the 'parse' entry instead");
			return new SectionNode[] {safeParseNode};
		}
		return new SectionNode[] {parseNode};
	}

	@SuppressWarnings("unchecked")
	protected boolean handleUsableEntry(SectionNode sectionNode, Map<T, List<Supplier<Boolean>>> usableSuppliers) {
		Script currentScript = SkriptUtil.getCurrentScript();
		for (Node usableNode : sectionNode) {
			String usableKey = usableNode.getKey();
			assert usableKey != null;

			Supplier<Boolean> supplier;
			if (usableKey.startsWith("custom event ")) {
				String customEventString = usableKey.substring("custom event ".length());
				VariableString variableString = VariableString.newInstance(
					customEventString.substring(1, customEventString.length() - 1));
				if (variableString == null || !variableString.isSimple()) {
					Skript.error("Custom event identifiers may only be simple strings");
					return false;
				} else {
					String identifier = variableString.toString(null);
					supplier = () -> {
						if (!getParser().isCurrentEvent(BukkitCustomEvent.class))
							return false;

						EventSyntaxInfo eventWhich = CustomEvent.lastWhich;
						return CustomEventUtils.getName(eventWhich).equalsIgnoreCase(identifier);
					};
				}
			} else {
				JavaType javaType = StructImport.lookup(currentScript, usableKey);
				Class<?> javaClass = javaType == null ? null : javaType.getJavaClass();
				if (javaClass == null || !Event.class.isAssignableFrom(javaClass)) {
					Skript.error(javaType + " is not a Bukkit event");
					return false;
				}
				Class<? extends Event> eventClass = (Class<? extends Event>) javaClass;

				supplier = () -> getParser().isCurrentEvent(eventClass);
			}
			whichInfo.forEach(which ->
				usableSuppliers.computeIfAbsent(which, (whichIndex) -> new ArrayList<>())
					.add(supplier)
			);
		}
		return true;
	}

	public T getFirstWhich() {
		return whichInfo.get(0);
	}

	public static EntryValidator.EntryValidatorBuilder customSyntaxValidator() {
		return EntryValidator.builder()
			.addEntryData(new PatternsEntryData("patterns", null, true))
			.addSection("parse", true)
			.addSection("safe parse", true) // Deprecated
			.addSection("usable in", true);
	}

}
