package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.skript.custom.CustomImport;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.skript.custom.event.*;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CustomExpressionSection extends CustomSyntaxSection<ExpressionSyntaxInfo> {
  static {
    CustomSyntaxSection.register("Define Expression", CustomExpressionSection.class,
        "[(2¦local)] [(1¦(plural|non(-|[ ])single))] expression <.+>",
        "[(2¦local)] [(1¦(plural|non(-|[ ])single))] expression",
        "[(2¦local)] [(1¦(plural|non(-|[ ])single))] %*classinfos% property <.+>");
  }

  private static final DataTracker<ExpressionSyntaxInfo> dataTracker = new DataTracker<>();

  static Map<ExpressionSyntaxInfo, Class<?>> returnTypes = new HashMap<>();
  static Map<ExpressionSyntaxInfo, Trigger> expressionHandlers = new HashMap<>();
  static Map<ExpressionSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
  static Map<ExpressionSyntaxInfo, Map<Changer.ChangeMode, Trigger>> changerHandlers = new HashMap<>();
  static Map<ExpressionSyntaxInfo, Map<Changer.ChangeMode, Class<?>[]>> changerTypes = new HashMap<>();
  static Map<ExpressionSyntaxInfo, String> loopOfs = new HashMap<>();
  static Map<ExpressionSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers = new HashMap<>();

  static {
    dataTracker.setSyntaxType("expression");

    // noinspection unchecked
    Skript.registerExpression(CustomExpression.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING);
    Optional<ExpressionInfo<?, ?>> info = StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(Skript.getExpressions(), Spliterator.ORDERED), false)
        .filter(i -> i.c == CustomExpression.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(returnTypes);
    dataTracker.addManaged(expressionHandlers);
    dataTracker.addManaged(changerHandlers);
    dataTracker.addManaged(changerTypes);
    dataTracker.addManaged(parserHandlers);
    dataTracker.addManaged(loopOfs);
    dataTracker.addManaged(usableSuppliers);
  }

  @Override
  protected DataTracker<ExpressionSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, SectionNode node) {
    String what;
    SectionNode patterns = (SectionNode) node.get("patterns");
    File script = (parseResult.mark & 2) == 2 ? SkriptUtil.getCurrentScript() : null;
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

    AtomicBoolean hasGetOrSet = new AtomicBoolean();
    boolean nodesOkay = handleEntriesAndSections(node,
      entryNode -> {
        String key = entryNode.getKey();
        assert key != null;

        if (key.equalsIgnoreCase("return type")) {
          String userReturnType = entryNode.getValue();
          Class<?> returnType = Classes.getClassFromUserInput(ScriptLoader.replaceOptions(userReturnType));
          whichInfo.forEach(which -> returnTypes.put(which, returnType));
          return true;
        }

        if (key.equalsIgnoreCase("loop of")) {
          String loopOf = entryNode.getValue();
          whichInfo.forEach(which -> loopOfs.put(which, loopOf));
          return true;
        }

        return false;
      },
      sectionNode -> {
        String key = sectionNode.getKey();
        assert key != null;

        if (key.equalsIgnoreCase("patterns")) {
          return true;
        }

        if (key.equalsIgnoreCase("get")) {
          ScriptLoader.setCurrentEvent("custom expression getter", ExpressionGetEvent.class);
          List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
          whichInfo.forEach(which ->
            expressionHandlers.put(which,
              new Trigger(SkriptUtil.getCurrentScript(), "get " + which.getPattern(), this, items)));

          hasGetOrSet.set(true);
          return true;
        }

        if (key.equalsIgnoreCase("parse")) {
          SyntaxParseEvent.register(this, sectionNode, whichInfo, parserHandlers);
          return true;
        }

        if (key.equalsIgnoreCase("usable in")) {
          return handleUsableSection(sectionNode, usableSuppliers);
        }

        for (Changer.ChangeMode mode : Changer.ChangeMode.values()) {
          String name = mode.name().replace("_", " ").toLowerCase();
          if (key.startsWith(name)) {
            String rawTypes = key.substring(name.length()).trim();

            ScriptLoader.setCurrentEvent("custom expression changer", ExpressionChangeEvent.class);
            List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
            whichInfo.forEach(which -> {
              Map<Changer.ChangeMode, Trigger> changerMap =
                changerHandlers.computeIfAbsent(which, k -> new HashMap<>());

              changerMap.put(mode,
                new Trigger(SkriptUtil.getCurrentScript(),
                  String.format("%s %s", name, which.getPattern()), this, items));
            });

            if (rawTypes.length() > 0) {
              Class<?>[] acceptedClasses = Arrays.stream(rawTypes.split(","))
                .map(String::trim)
                .map(SkriptUtil::getUserClassInfoAndPlural)
                .map(meta -> {
                  ClassInfo<?> ci = meta.getFirst();
                  boolean plural = meta.getSecond();

                  if (plural) {
                    return JavaUtil.getArrayClass(ci.getC());
                  }

                  return ci.getC();
                })
                .toArray(Class[]::new);

              whichInfo.forEach(which -> {
                changerTypes.computeIfAbsent(which, k -> new HashMap<>())
                  .put(mode, acceptedClasses);
              });
            }

            hasGetOrSet.set(true);
            return true;
          }
        }

        return false;
      });

    if (!nodesOkay)
      return false;

    if (!hasGetOrSet.get())
      Skript.warning("Custom expressions are useless without a get / change section");

    return true;
  }

  public static ExpressionSyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }

}

