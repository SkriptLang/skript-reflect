package org.skriptlang.reflect.syntax.effect.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.effect.EffectSyntaxInfo;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class StructCustomEffect extends CustomSyntaxStructure<EffectSyntaxInfo> {

	public static boolean customEffectsUsed = false;

	static {
		String[] syntax = {
			"[:local] effect <.+>",
			"[:local] effect"
		};
		Skript.registerStructure(StructCustomEffect.class, customSyntaxValidator()
			.addSection("trigger", false)
			.build(),
			syntax
		);
	}

	public static final DataTracker<EffectSyntaxInfo> dataTracker = new DataTracker<>();

	static final Map<EffectSyntaxInfo, Trigger> effectHandlers = new HashMap<>();
	static final Map<EffectSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
	static final Map<EffectSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers = new HashMap<>();
	static final Map<EffectSyntaxInfo, Boolean> parseSectionLoaded = new HashMap<>();

	static {
		Skript.registerEffect(CustomEffect.class, DEFAULT_PATTERN);
		Optional<SyntaxInfo<? extends Effect>> info = SkriptMirror.getAddonInstance().syntaxRegistry().syntaxes(SyntaxRegistry.EFFECT).stream()
			.filter(i -> i.type() == CustomEffect.class)
			.findFirst();
		info.ifPresent(dataTracker::setInfo);

		dataTracker.setSyntaxKey(SyntaxRegistry.EFFECT);
		dataTracker.addManaged(effectHandlers);
		dataTracker.addManaged(parserHandlers);
		dataTracker.addManaged(usableSuppliers);
		dataTracker.addManaged(parseSectionLoaded);
	}

	private SectionNode parseNode;

	@Override
	public DataTracker<EffectSyntaxInfo> getDataTracker() {
		return dataTracker;
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
						 EntryContainer entryContainer) {
		customEffectsUsed = true;

		List<String> patterns = entryContainer.getOptional("patterns", List.class, false);
		Script script = parseResult.hasTag("local") ? SkriptUtil.getCurrentScript() : null;

		if (matchedPattern != 1 && patterns != null) {
			Skript.error("A custom effect with an inline pattern cannot have a 'patterns' entry too");
			return false;
		}

		switch (matchedPattern) {
			case 0: // effect with an inline pattern
				register(EffectSyntaxInfo.create(script, parseResult.regexes.get(0).group(), 1));
				break;
			case 1: // effect with a 'patterns' entry
				if (patterns == null) {
					Skript.error("A custom effect without an inline pattern must have a 'patterns' entry");
					return false;
				}

				int i = 1;
				for (String pattern : patterns) {
					register(EffectSyntaxInfo.create(script, pattern, i++));
				}
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

		SectionNode triggerNode = getEntryContainer().get("trigger", SectionNode.class, false);
		SkriptLogger.setNode(triggerNode);
		getParser().setCurrentEvent("custom effect trigger", EffectTriggerEvent.class);
		List<TriggerItem> items = SkriptUtil.getItemsFromNode(triggerNode);
		whichInfo.forEach(which -> effectHandlers.put(which,
			new Trigger(getParser().getCurrentScript(), "effect " + which, new SimpleEvent(), items))
		);
		SkriptLogger.setNode(null);

		return true;
	}

	public static EffectSyntaxInfo lookup(Script script, int matchedPattern) {
		return dataTracker.lookup(script, matchedPattern);
	}

}
