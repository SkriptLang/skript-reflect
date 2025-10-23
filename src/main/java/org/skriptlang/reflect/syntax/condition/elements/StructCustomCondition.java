package org.skriptlang.reflect.syntax.condition.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Utils;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.condition.ConditionSyntaxInfo;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StructCustomCondition extends CustomSyntaxStructure<ConditionSyntaxInfo> {

	public static boolean customConditionsUsed = false;

	static {
		String[] syntax = {
			"[:local] condition <.+>",
			"[:local] condition",
			"[:local] %*classinfos% property condition <.+>"
		};
		Skript.registerStructure(StructCustomCondition.class, customSyntaxValidator()
			.addSection("check", false)
			.build(),
			syntax
		);
	}

	public static final DataTracker<ConditionSyntaxInfo> dataTracker = new DataTracker<>();

	static final Map<ConditionSyntaxInfo, Trigger> conditionHandlers = new HashMap<>();
	static final Map<ConditionSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
	static final Map<ConditionSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers = new HashMap<>();
	static final Map<ConditionSyntaxInfo, Boolean> parseSectionLoaded = new HashMap<>();

	static {
		Skript.registerCondition(CustomCondition.class, DEFAULT_PATTERN);
		Optional<SyntaxInfo<? extends Condition>> info = SkriptMirror.getAddonInstance().syntaxRegistry().syntaxes(SyntaxRegistry.CONDITION).stream()
				.filter(i -> i.type() == CustomCondition.class)
				.findFirst();
		info.ifPresent(dataTracker::setInfo);
		dataTracker.setSyntaxKey(SyntaxRegistry.CONDITION);

		dataTracker.addManaged(conditionHandlers);
		dataTracker.addManaged(parserHandlers);
		dataTracker.addManaged(usableSuppliers);
		dataTracker.addManaged(parseSectionLoaded);
	}

	private SectionNode parseNode;

	@Override
	public DataTracker<ConditionSyntaxInfo> getDataTracker() {
		return dataTracker;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
						EntryContainer entryContainer) {
		customConditionsUsed = true;

		List<String> patterns = entryContainer.getOptional("patterns", List.class, false);
		Script script = parseResult.hasTag("local") ? SkriptUtil.getCurrentScript() : null;

		if (matchedPattern != 1 && patterns != null) {
			Skript.error("A custom condition with an inline pattern cannot have a 'patterns' entry too");
			return false;
		}

		switch (matchedPattern) {
			case 0: // condition with an inline pattern
				String pattern = parseResult.regexes.get(0).group();
				register(ConditionSyntaxInfo.create(script, pattern, 1, false, false));
				break;
			case 1: // condition with a 'patterns' entry
				if (patterns == null) {
					Skript.error("A custom condition without an inline pattern must have a 'patterns' entry");
					return false;
				}

				int i = 1;
				for (String p : patterns) {
					register(ConditionSyntaxInfo.create(script, p, i++, false, false));
				}
				break;
			case 2: // property condition
				String property = parseResult.regexes.get(0).group();
				String type = Arrays.stream(((Literal<ClassInfo<?>>) args[0]).getArray())
					.map(ClassInfo::getCodeName)
					.map(codeName -> {
						boolean isPlural = Utils.getEnglishPlural(codeName).getSecond();

						if (!isPlural) {
							return Utils.toEnglishPlural(codeName);
						}

						return codeName;
					})
					.collect(Collectors.joining("/"));
				register(ConditionSyntaxInfo.create(script, "%" + type + "% (is|are) " + property, 1, false, true));
				register(ConditionSyntaxInfo.create(script, "%" + type + "% (isn't|is not|aren't|are not) " + property, 1, true, true));
				break;
		}

		return checkHasPatterns();
	}

	@Override
	public boolean preLoad() {
		super.preLoad();
		EntryContainer entryContainer = getEntryContainer();

		SectionNode[] parseNode = getParseNode();
		if (parseNode == null)
			return false;
		this.parseNode = parseNode[0];
		whichInfo.forEach(which -> parseSectionLoaded.put(which, this.parseNode == null));

		SectionNode usableInNode = entryContainer.getOptional("usable in", SectionNode.class, false);
		return usableInNode == null || handleUsableEntry(usableInNode, usableSuppliers);
	}

	@Override
	public boolean load() {
		if (parseNode != null) {
			SkriptLogger.setNode(parseNode);
			SyntaxParseEvent.register(parseNode, whichInfo, parserHandlers);

			whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
		}

		SectionNode checkNode = getEntryContainer().get("check", SectionNode.class, false);
		SkriptLogger.setNode(checkNode);
		getParser().setCurrentEvent("custom condition check", ConditionCheckEvent.class);
		List<TriggerItem> items = SkriptUtil.getItemsFromNode(checkNode);
		whichInfo.forEach(which -> conditionHandlers.put(which,
			new Trigger(getParser().getCurrentScript(), "condition " + which, new SimpleEvent(), items))
		);
		SkriptLogger.setNode(null);

		return true;
	}

	public static ConditionSyntaxInfo lookup(Script script, int matchedPattern) {
		return dataTracker.lookup(script, matchedPattern);
	}

}
