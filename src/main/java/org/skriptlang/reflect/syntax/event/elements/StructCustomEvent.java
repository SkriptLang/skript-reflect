package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventValuesEntryData;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StructCustomEvent extends CustomSyntaxStructure<EventSyntaxInfo> {

	public static boolean customEventsUsed = false;

	static {
		Skript.registerStructure(StructCustomEvent.class, customSyntaxValidator()
			.addEntry("pattern", null, true)
			.addEntryData(new EventValuesEntryData("event values", null, true) {
				@Override
				public boolean canCreateWith(String node) {
					return super.canCreateWith(node) || node.startsWith(getKey().replace(' ', '-') + getSeparator());
				}
			})
			.addSection("check", true)
			.build(),
			"[:local] [custom] event %string%"
		);
	}

	private static final DataTracker<EventSyntaxInfo> dataTracker = new DataTracker<>();

	static final Map<EventSyntaxInfo, String> nameValues = new HashMap<>();
	static final Map<EventSyntaxInfo, List<ClassInfo<?>>> eventValueTypes = new HashMap<>();
	static final Map<EventSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
	static final Map<EventSyntaxInfo, Trigger> eventHandlers = new HashMap<>();
	static final Map<EventSyntaxInfo, Boolean> parseSectionLoaded = new HashMap<>();

	static {
		Skript.registerEvent("custom event", CustomEvent.class, BukkitCustomEvent.class, DEFAULT_PATTERN);
		Optional<BukkitSyntaxInfos.Event<?>> info = SkriptMirror.getAddonInstance().syntaxRegistry().syntaxes(BukkitSyntaxInfos.Event.KEY).stream()
			.filter(i -> i.type() == CustomEvent.class)
			.findFirst();
		info.ifPresent(dataTracker::setInfo);

		dataTracker.setSyntaxKey(BukkitSyntaxInfos.Event.KEY);
		dataTracker.addManaged(nameValues);
		dataTracker.addManaged(eventValueTypes);
		dataTracker.addManaged(parserHandlers);
		dataTracker.addManaged(eventHandlers);
		dataTracker.addManaged(parseSectionLoaded);
	}

	private SectionNode parseNode;

	@Override
	protected DataTracker<EventSyntaxInfo> getDataTracker() {
		return dataTracker;
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
						 EntryContainer entryContainer) {
		customEventsUsed = true;

		Script script = parseResult.hasTag("local") ? SkriptUtil.getCurrentScript() : null;

		List<String> patterns = entryContainer.getOptional("patterns", List.class, false);
		String patternNode = entryContainer.getOptional("pattern", String.class, false);
		if (patterns != null && patternNode != null) {
			Skript.error("A custom event may not have both a 'patterns' and 'pattern' entries");
			return false;
		} else if (patternNode != null) {
			patterns = Collections.singletonList(patternNode);
		}

		if (patterns == null || patterns.isEmpty()) {
			// Always false. Used for the error
			return checkHasPatterns();
		}

		int i = 1;
		for (String pattern : patterns) {
			register(EventSyntaxInfo.create(script, pattern, i++));
		}

		String name = (String) args[0].getSingle();
		if (nameValues.values().stream().anyMatch(name::equalsIgnoreCase)) {
			Skript.error("There is already a custom event with that name");
			return false;
		}

		whichInfo.forEach(which -> nameValues.put(which, name));

		// Register the custom events during #init, rather than #preLoad
		super.preLoad();
		return true;
	}

	@Override
	public boolean preLoad() {
		SectionNode[] parseNode = getParseNode();
		if (parseNode == null)
			return false;
		this.parseNode = parseNode[0];
		whichInfo.forEach(which -> parseSectionLoaded.put(which, this.parseNode == null));

		return true;
	}

	@Override
	public boolean load() {
		EntryContainer entryContainer = getEntryContainer();

		List<ClassInfo<?>> classInfoList = entryContainer.getOptional("event values", List.class, false);
		if (classInfoList != null) {
			SkriptReflection.replaceEventValues(classInfoList);
			whichInfo.forEach(which -> eventValueTypes.put(which, classInfoList));
		}

		if (parseNode != null) {
			SkriptLogger.setNode(parseNode);
			SyntaxParseEvent.register(parseNode, whichInfo, parserHandlers);

			whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
		}

		SectionNode checkNode = entryContainer.getOptional("check", SectionNode.class, false);
		if (checkNode != null) {
			SkriptLogger.setNode(checkNode);

			getParser().setCurrentEvent("custom event trigger", EventTriggerEvent.class);
			CustomEvent.setLastWhich(whichInfo.get(0));
			List<TriggerItem> items = SkriptUtil.getItemsFromNode(checkNode);
			CustomEvent.setLastWhich(null);
			whichInfo.forEach(which -> eventHandlers.put(which,
				new Trigger(getParser().getCurrentScript(), "event " + which, new SimpleEvent(), items))
			);
		}
		SkriptLogger.setNode(null);

		return true;
	}

	public static EventSyntaxInfo lookup(Script script, int matchedPattern) {
		return dataTracker.lookup(script, matchedPattern);
	}

}
