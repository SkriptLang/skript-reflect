package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.skriptlang.reflect.syntax.expression.ChangerEntryData;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionSyntaxInfo;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StructCustomExpression extends CustomSyntaxStructure<ExpressionSyntaxInfo> {

	public static boolean customExpressionsUsed = false;

	static {
		String[] syntax = {
			"[:local] [plural:(plural|non[-| ]single)] expression <.+>",
			"[:local] [plural:(plural|non[-| ]single)] expression",
			"[:local] [plural:(plural|non[-| ]single)] %*classinfos% property <.+>"
		};
		EntryValidator.EntryValidatorBuilder builder = customSyntaxValidator()
			.addEntryData(new KeyValueEntryData<Class<?>>("return type", null, true) {
				@Override
				@Nullable
				protected Class<?> getValue(String value) {
					Class<?> returnType = Classes.getClassFromUserInput(value);
					if (returnType == null)
						Skript.error("The given return type doesn't exist");
					return returnType;
				}
			})
			.addEntry("loop of", null, true)
			.addSection("get", false);
		Arrays.stream(ChangeMode.values())
			.sorted((mode1, mode2) -> {
				long words1 = StringUtils.count(mode1.toString(), '_');
				long words2 = StringUtils.count(mode2.toString(), '_');
				return Long.compare(words2, words1);
			})
			.map(mode -> mode.toString().replace('_', ' ').toLowerCase(Locale.ENGLISH))
			.forEach(name -> builder.addEntryData(new ChangerEntryData(name, true)));
		Skript.registerStructure(StructCustomExpression.class, builder.build(), syntax);
	}

	public static final DataTracker<ExpressionSyntaxInfo> dataTracker = new DataTracker<>();

	static final Map<ExpressionSyntaxInfo, Class<?>> returnTypes = new HashMap<>();
	static final Map<ExpressionSyntaxInfo, Trigger> expressionHandlers = new HashMap<>();
	static final Map<ExpressionSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
	static final Map<ExpressionSyntaxInfo, List<ChangeMode>> hasChanger = new HashMap<>();
	static final Map<ExpressionSyntaxInfo, Map<ChangeMode, Trigger>> changerHandlers = new HashMap<>();
	static final Map<ExpressionSyntaxInfo, Map<ChangeMode, Class<?>[]>> changerTypes = new HashMap<>();
	static final Map<ExpressionSyntaxInfo, String> loopOfs = new HashMap<>();
	static final Map<ExpressionSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers = new HashMap<>();
	static final Map<ExpressionSyntaxInfo, Boolean> parseSectionLoaded = new HashMap<>();

	static {
		// noinspection unchecked
		Skript.registerExpression(CustomExpression.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, DEFAULT_PATTERN);
		Optional<SyntaxInfo<?>> info = SkriptMirror.getAddonInstance().syntaxRegistry().elements().stream()
				.filter(i -> Expression.class.isAssignableFrom(i.type()))
				.filter(i -> i.type() == CustomExpression.class)
				.findFirst();
		info.ifPresent(dataTracker::setInfo);

		dataTracker.setSyntaxKey(SyntaxRegistry.EXPRESSION);
		dataTracker.addManaged(returnTypes);
		dataTracker.addManaged(expressionHandlers);
		dataTracker.addManaged(hasChanger);
		dataTracker.addManaged(changerHandlers);
		dataTracker.addManaged(changerTypes);
		dataTracker.addManaged(parserHandlers);
		dataTracker.addManaged(loopOfs);
		dataTracker.addManaged(usableSuppliers);
		dataTracker.addManaged(parseSectionLoaded);
	}

	private final Map<ChangeMode, SectionNode> changerNodes = new HashMap<>();
	private SectionNode parseNode;

	@Override
	protected DataTracker<ExpressionSyntaxInfo> getDataTracker() {
		return dataTracker;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
						EntryContainer entryContainer) {
		customExpressionsUsed = true;

		List<String> patterns = entryContainer.getOptional("patterns", List.class, false);
		Script script = parseResult.hasTag("local") ? SkriptUtil.getCurrentScript() : null;
		boolean alwaysPlural = parseResult.hasTag("plural");

		if (matchedPattern != 1 && patterns != null) {
			Skript.error("A custom expression with an inline pattern cannot have a 'patterns' entry too");
			return false;
		}

		switch (matchedPattern) {
			case 0: // expression with an inline pattern
				String pattern = parseResult.regexes.get(0).group();
				register(ExpressionSyntaxInfo.create(script, pattern, 1, alwaysPlural, false, false));
				break;
			case 1: // expression with a 'patterns' entry
				if (patterns == null) {
					Skript.error("A custom expression without an inline pattern must have a 'patterns' entry");
					return false;
				}

				int i = 1;
				for (String p : patterns) {
					register(ExpressionSyntaxInfo.create(script, p, i++, alwaysPlural, false, false));
				}
				break;
			case 2: // property expression
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

				if (!alwaysPlural) {
					type = "$" + type;
				}

				register(
					ExpressionSyntaxInfo.create(script, "[the] " + property + " of %" + type + "%", 1, alwaysPlural, true, true));
				register(ExpressionSyntaxInfo.create(script, "%" + type + "%'[s] " + property, 1, alwaysPlural, false, true));
				break;
		}

		return true;
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

		Class<?> returnType = entryContainer.getOptional("return type", Class.class, false);
		if (returnType != null)
			whichInfo.forEach(which -> returnTypes.put(which, returnType));

		String loopOf = entryContainer.getOptional("loop of", String.class, false);
		if (loopOf != null)
			whichInfo.forEach(which -> loopOfs.put(which, loopOf));

		SectionNode usableInNode = entryContainer.getOptional("usable in", SectionNode.class, false);
		if (usableInNode != null && !handleUsableEntry(usableInNode, usableSuppliers))
			return false;

		for (ChangeMode mode : ChangeMode.values()) {
			String name = mode.toString().replace('_', ' ').toLowerCase(Locale.ENGLISH);
			NonNullPair<SectionNode, Class<?>[]> pair = entryContainer.getOptional(name, NonNullPair.class, false);
			if (pair == null)
				continue;
			changerNodes.put(mode, pair.getFirst());

			whichInfo.forEach(which -> {
				List<ChangeMode> hasChangerList =
					hasChanger.computeIfAbsent(which, k -> new ArrayList<>());

				hasChangerList.add(mode);
			});


			if (pair.getSecond().length == 0)
				continue;
			whichInfo.forEach(which -> changerTypes.computeIfAbsent(which, k -> new HashMap<>())
				.put(mode, pair.getSecond())
			);
		}

		return true;
	}

	@Override
	public boolean load() {
		if (parseNode != null) {
			SkriptLogger.setNode(parseNode);
			SyntaxParseEvent.register(parseNode, whichInfo, parserHandlers);

			whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
		}

		SectionNode getNode = getEntryContainer().get("get", SectionNode.class, false);
		SkriptLogger.setNode(getNode);
		{
			getParser().setCurrentEvent("custom expression getter", ExpressionGetEvent.class);
			List<TriggerItem> items = SkriptUtil.getItemsFromNode(getNode);
			whichInfo.forEach(which -> expressionHandlers.put(which,
				new Trigger(getParser().getCurrentScript(), "get " + which.getPattern(), new SimpleEvent(), items))
			);
		}

		changerNodes.forEach((changeMode, node) -> {
			SkriptLogger.setNode(node);
			getParser().setCurrentEvent("custom expression changer", ExpressionChangeEvent.class);
			List<TriggerItem> items = SkriptUtil.getItemsFromNode(node);
			whichInfo.forEach(which -> {
				Map<ChangeMode, Trigger> changerMap = changerHandlers.computeIfAbsent(which, k -> new HashMap<>());

				String name = changeMode.toString().toLowerCase(Locale.ENGLISH).replace('_', ' ');
				changerMap.put(changeMode, new Trigger(
					getParser().getCurrentScript(), String.format("%s %s", name, which.getPattern()), new SimpleEvent(), items
				));
			});
		});

		SkriptLogger.setNode(null);
		return true;
	}

	public static ExpressionSyntaxInfo lookup(Script script, int matchedPattern) {
		return dataTracker.lookup(script, matchedPattern);
	}

}

