package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import com.btk5h.skriptmirror.Util;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;

import java.util.*;
import java.util.stream.StreamSupport;

public class CustomExpressionSection extends SelfRegisteringSkriptEvent {
  static {
    //noinspection unchecked
    Skript.registerEvent("*Define Expression", CustomExpressionSection.class, CustomSyntaxEvent.class,
        "[(1¦(plural|non(-|[ ])single|multi[ple]))] expression <.+>",
        "%*classinfo% property <.+>");

    //noinspection unchecked
    Skript.registerExpression(CustomExpression.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING);
    Optional<ExpressionInfo<?, ?>> info = StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(Skript.getExpressions(), Spliterator.ORDERED), false)
        .filter(i -> i.c == CustomExpression.class)
        .findFirst();

    if (info.isPresent()) {
      CustomExpressionSection.thisInfo = info.get();
    } else {
      Skript.warning("Could not find custom expression class. Custom expressions will not work.");
    }
  }

  private static SyntaxElementInfo<?> thisInfo;
  private static final SectionValidator EXPRESSION_DECLARATION;

  static {
    EXPRESSION_DECLARATION =
        new SectionValidator()
            .addEntry("return type", true)
            .addSection("get", true);
    Arrays.stream(Changer.ChangeMode.values())
        .map(mode -> mode.name().replace("_", " ").toLowerCase())
        .forEach(mode -> CustomExpressionSection.EXPRESSION_DECLARATION.addSection(mode, true));
  }

  static List<String> expressions = new ArrayList<>();
  static Map<String, SyntaxInfo> expressionInfos = new HashMap<>();
  static Map<SyntaxInfo, Class<?>> returnTypes = new HashMap<>();
  static Map<SyntaxInfo, Trigger> expressionHandlers = new HashMap<>();
  static Map<SyntaxInfo, Map<Changer.ChangeMode, Trigger>> changerHandlers =
      new HashMap<>();

  private List<SyntaxInfo> whiches = new ArrayList<>();

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Literal<?>[] args, int matchedPattern,
                      SkriptParser.ParseResult parseResult) {
    String what = parseResult.regexes.get(0).group();
    switch (matchedPattern) {
      case 0:
        whiches.add(SyntaxInfo.create(what, (parseResult.mark & 1) == 1, false, false));
        break;
      case 1:
        String fromType = ((Literal<ClassInfo>) args[0]).getSingle().getCodeName();
        whiches.add(SyntaxInfo.create("[the] " + what + " of %$" + fromType + "s%", false, true, true));
        whiches.add(SyntaxInfo.create("%$" + fromType + "s%'[s] " + what, false, false, true));
        break;
    }

    whiches.forEach(which -> {
      String pattern = which.getPattern();
      if (!expressions.contains(pattern)) {
        if (expressions.contains(pattern)) {
          Skript.error(String.format("The custom expression '%s' already has a handler.",
              pattern));
        } else {
          expressions.add(pattern);
          expressionInfos.put(pattern, which);
        }
      }
    });

    SectionNode node = (SectionNode) SkriptLogger.getNode();
    node.convertToEntries(0);

    boolean ok = EXPRESSION_DECLARATION.validate(node);

    if (!ok) {
      unregister(null);
      return false;
    }

    register(node);

    return true;
  }

  @SuppressWarnings("unchecked")
  private void register(SectionNode node) {
    String userReturnType = node.getValue("return type");
    if (userReturnType != null) {
      Class returnType =
          Classes.getClassFromUserInput(ScriptLoader.replaceOptions(userReturnType));
      whiches.forEach(which -> returnTypes.put(which, returnType));
    }

    ScriptLoader.setCurrentEvent("custom expression getter", ExpressionGetEvent.class);
    Util.getItemsFromNode(node, "get").ifPresent(items ->
        whiches.forEach(which ->
            expressionHandlers.put(which,
                new Trigger(ScriptLoader.currentScript.getFile(), "get " + which.getPattern(),
                    this, items))
        )
    );

    Arrays.stream(Changer.ChangeMode.values())
        .forEach(mode -> {
          String name = mode.name().replace("_", " ").toLowerCase();
          ScriptLoader.setCurrentEvent("custom expression changer", ExpressionChangeEvent.class);
          Util.getItemsFromNode(node, name).ifPresent(items ->
              whiches.forEach(which -> {
                    Map<Changer.ChangeMode, Trigger> changerMap =
                        changerHandlers.computeIfAbsent(which, k -> new HashMap<>());
                    changerMap.put(mode,
                        new Trigger(ScriptLoader.currentScript.getFile(),
                            String.format("%s %s", name, which.getPattern()), this, items));
                  }
              )
          );
        });

    Util.clearSectionNode(node);
    updateExpressions();
  }

  @Override
  public void register(Trigger t) {
  }

  @Override
  public void unregister(Trigger t) {
    whiches.forEach(which -> {
      expressionHandlers.remove(which);
      changerHandlers.remove(which);
      returnTypes.remove(which);
      expressions.remove(which.getPattern());
      expressionInfos.remove(which.getPattern());
    });
    updateExpressions();
  }

  @Override
  public void unregisterAll() {
    expressions.clear();
    expressionInfos.clear();
    returnTypes.clear();
    expressionHandlers.clear();
    changerHandlers.clear();
    updateExpressions();
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "expression: " + whiches.toString();
  }

  private static void updateExpressions() {
    Util.setPatterns(thisInfo, expressions.toArray(new String[0]));
  }
}
