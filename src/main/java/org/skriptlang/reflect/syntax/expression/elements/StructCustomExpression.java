package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.reflect.syntax.expression.ChangerEntryData;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionSyntaxInfo;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;
import org.skriptlang.skript.lang.script.Script;

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
      "[(2¦local)] [(1¦(plural|non(-|[ ])single))] expression <.+>",
      "[(2¦local)] [(1¦(plural|non(-|[ ])single))] expression",
      "[(2¦local)] [(1¦(plural|non(-|[ ])single))] %*classinfos% property <.+>"
    };
    EntryValidator.EntryValidatorBuilder builder = customSyntaxValidator()
        .addEntryData(new KeyValueEntryData<Class<?>>("return type", null, true) {
          @Override
          protected @Nullable Class<?> getValue(String value) {
            Class<?> returnType = Classes.getClassFromUserInput(value);
            if (returnType == null)
              Skript.error("The given return type doesn't exist");
            return returnType;
          }
        })
        .addEntry("loop of", null, true)
        .addSection("get", true);
    Arrays.stream(Changer.ChangeMode.values())
        .sorted((mode1, mode2) -> {
          long words1 = mode1.toString().chars().filter(c -> c == '_').count();
          long words2 = mode2.toString().chars().filter(c -> c == '_').count();
          return Long.compare(words2, words1);
        })
        .forEach(mode -> {
          String name = mode.toString().replace('_', ' ').toLowerCase(Locale.ENGLISH);
          builder.addEntryData(new ChangerEntryData(name, true));
        });
    Skript.registerStructure(StructCustomExpression.class, builder.build(), syntax);
  }

  private static final DataTracker<ExpressionSyntaxInfo> dataTracker = new DataTracker<>();

  static final Map<ExpressionSyntaxInfo, Class<?>> returnTypes = new HashMap<>();
  static final Map<ExpressionSyntaxInfo, Trigger> expressionHandlers = new HashMap<>();
  static final Map<ExpressionSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
  static final Map<ExpressionSyntaxInfo, List<Changer.ChangeMode>> hasChanger = new HashMap<>();
  static final Map<ExpressionSyntaxInfo, Map<Changer.ChangeMode, Trigger>> changerHandlers = new HashMap<>();
  static final Map<ExpressionSyntaxInfo, Map<Changer.ChangeMode, Class<?>[]>> changerTypes = new HashMap<>();
  static final Map<ExpressionSyntaxInfo, String> loopOfs = new HashMap<>();
  static final Map<ExpressionSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers = new HashMap<>();
  static final Map<ExpressionSyntaxInfo, Boolean> parseSectionLoaded = new HashMap<>();

  static {
    // noinspection unchecked
    Skript.registerExpression(CustomExpression.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING);
    Optional<ExpressionInfo<?, ?>> info = StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(Skript.getExpressions(), Spliterator.ORDERED), false)
        .filter(i -> i.c == CustomExpression.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);

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

  private final Map<Changer.ChangeMode, SectionNode> changerNodes = new HashMap<>();
  private SectionNode parseNode, getNode;

  @Override
  protected DataTracker<ExpressionSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         EntryContainer entryContainer) {
    customExpressionsUsed = true;

    String what;
    SectionNode patterns = entryContainer.getOptional("patterns", SectionNode.class, false);
    Script script = (parseResult.mark & 2) == 2 ? SkriptUtil.getCurrentScript() : null;
    boolean alwaysPlural = (parseResult.mark & 1) == 1;

    switch (matchedPattern) {
      case 0:
        what = parseResult.regexes.get(0).group();
        register(ExpressionSyntaxInfo.create(script, what, 1, alwaysPlural, false, false));
        break;
      case 1:
        if (patterns == null) {
          Skript.error("Custom expressions without inline patterns must have a patterns section.");
          return false;
        }

        int i = 1;
        for (Node subNode : patterns) {
          register(
            ExpressionSyntaxInfo.create(script, subNode.getKey(), i++, alwaysPlural, false, false));
        }
        break;
      case 2:
        what = parseResult.regexes.get(0).group();
        String fromType = Arrays.stream(((Literal<ClassInfo<?>>) args[0]).getArray())
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
          fromType = "$" + fromType;
        }

        register(
          ExpressionSyntaxInfo.create(script, "[the] " + what + " of %" + fromType + "%", 1, alwaysPlural, true, true));
        register(
          ExpressionSyntaxInfo.create(script, "%" + fromType + "%'[s] " + what, 1, alwaysPlural, false, true));
        break;
    }

    if (matchedPattern != 1 && patterns != null) {
      Skript.error("Custom expressions with inline patterns may not have a patterns section.");
      return false;
    }

    return true;
  }

  @Override
  public boolean preLoad() {
    super.preLoad();

    EntryContainer entryContainer = getEntryContainer();
    parseNode = entryContainer.getOptional("parse", SectionNode.class, false);
    SectionNode safeParseNode = entryContainer.getOptional("safe parse", SectionNode.class, false);
    if (parseNode != null) {
      if (safeParseNode != null) {
        Skript.error("You can't have two parse sections");
        return false;
      }
      whichInfo.forEach(which -> parseSectionLoaded.put(which, false));
    } else {
      if (safeParseNode != null) {
        SyntaxParseEvent.register(safeParseNode, whichInfo, parserHandlers);

        whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
      }
    }

    Class<?> returnType = entryContainer.getOptional("return type", Class.class, false);
    if (returnType != null)
      whichInfo.forEach(which -> returnTypes.put(which, returnType));

    String loopOf = entryContainer.getOptional("loop of", String.class, false);
    if (loopOf != null)
      whichInfo.forEach(which -> loopOfs.put(which, loopOf));

    getNode = entryContainer.getOptional("get", SectionNode.class, false);

    SectionNode usableInNode = entryContainer.getOptional("usable in", SectionNode.class, false);
    if (usableInNode != null && !handleUsableSection(usableInNode, usableSuppliers))
      return false;

    for (Changer.ChangeMode mode : Changer.ChangeMode.values()) {
      String name = mode.toString().replace('_', ' ').toLowerCase(Locale.ENGLISH);
      NonNullPair<SectionNode, Class<?>[]> pair = entryContainer.getOptional(name, NonNullPair.class, false);
      if (pair == null)
        continue;
      changerNodes.put(mode, pair.getFirst());

      whichInfo.forEach(which -> {
        List<Changer.ChangeMode> hasChangerList =
            hasChanger.computeIfAbsent(which, k -> new ArrayList<>());

        hasChangerList.add(mode);
      });


      if (pair.getSecond().length == 0)
        continue;
      whichInfo.forEach(which -> changerTypes.computeIfAbsent(which, k -> new HashMap<>())
          .put(mode, pair.getSecond())
      );
    }

    if (getNode == null && changerNodes.isEmpty()) {
      Skript.error("Custom expressions are useless without a get / change section");
    } else if (getNode == null) {
      Skript.error("Custom expressions require a 'get' section");
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

    if (getNode != null) {
      SkriptLogger.setNode(getNode);
      getParser().setCurrentEvent("custom expression getter", ExpressionGetEvent.class);
      List<TriggerItem> items = SkriptUtil.getItemsFromNode(getNode);
      whichInfo.forEach(which ->
          expressionHandlers.put(which,
              new Trigger(getParser().getCurrentScript(), "get " + which.getPattern(), new SimpleEvent(), items)));
    }

    changerNodes.forEach((changeMode, node) -> {
      SkriptLogger.setNode(node);
      getParser().setCurrentEvent("custom expression changer", ExpressionChangeEvent.class);
      List<TriggerItem> items = SkriptUtil.getItemsFromNode(node);
      whichInfo.forEach(which -> {
        Map<Changer.ChangeMode, Trigger> changerMap =
            changerHandlers.computeIfAbsent(which, k -> new HashMap<>());

        String name = changeMode.toString().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        changerMap.put(changeMode,
            new Trigger(getParser().getCurrentScript(),
                String.format("%s %s", name, which.getPattern()), new SimpleEvent(), items));
      });
    });

    SkriptLogger.setNode(null);
    return true;
  }

  public static ExpressionSyntaxInfo lookup(Script script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }

}

