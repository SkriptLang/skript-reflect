package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.registrations.Classes;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.io.File;
import java.util.*;
import java.util.stream.StreamSupport;

public class CustomExpressionSection extends CustomSyntaxSection<SyntaxInfo> {
  static {
    //noinspection unchecked
    CustomSyntaxSection.register("Define Expression", CustomExpressionSection.class,
        "[(2¦local)] [(1¦(plural|non(-|[ ])single))] expression <.+>",
        "[(2¦local)] [(1¦(plural|non(-|[ ])single))] expression",
        "[(2¦local)] %*classinfo% property <.+>");
  }

  private static DataTracker<SyntaxInfo> dataTracker = new DataTracker<>();

  static Map<SyntaxInfo, Class<?>> returnTypes = new HashMap<>();
  static Map<SyntaxInfo, Trigger> expressionHandlers = new HashMap<>();
  static Map<SyntaxInfo, Trigger> parserHandlers = new HashMap<>();
  static Map<SyntaxInfo, Map<Changer.ChangeMode, Trigger>> changerHandlers = new HashMap<>();

  static {
    dataTracker.setSyntaxType("expression");

    dataTracker.getValidator()
        .addEntry("return type", true)
        .addSection("get", true)
        .addSection("patterns", true)
        .addSection("parse", true);
    Arrays.stream(Changer.ChangeMode.values())
        .map(mode -> mode.name().replace("_", " ").toLowerCase())
        .forEach(mode -> dataTracker.getValidator().addSection(mode, true));

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
    dataTracker.addManaged(parserHandlers);
  }

  @Override
  protected DataTracker<SyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean init(Literal[] args, int matchedPattern, SkriptParser.ParseResult parseResult, SectionNode node) {
    String what;
    SectionNode patterns = (SectionNode) node.get("patterns");
    File script = (parseResult.mark & 2) == 2 ? SkriptUtil.getCurrentScript() : null;

    switch (matchedPattern) {
      case 0:
        what = parseResult.regexes.get(0).group();
        register(SyntaxInfo.create(script, what, (parseResult.mark & 1) == 1, false, false));
        break;
      case 1:
        if (patterns == null) {
          Skript.error("Custom expressions without inline patterns must have a patterns section.");
          return false;
        }

        patterns.forEach(subNode ->
            register(SyntaxInfo.create(script, subNode.getKey(), (parseResult.mark & 1) == 1, false, false)));
        break;
      case 2:
        what = parseResult.regexes.get(0).group();
        String fromType = ((Literal<ClassInfo>) args[0]).getSingle().getCodeName();
        register(SyntaxInfo.create(script, "[the] " + what + " of %$" + fromType + "s%", false, true, true));
        register(SyntaxInfo.create(script, "%$" + fromType + "s%'[s] " + what, false, false, true));
        break;
    }

    if (matchedPattern != 1 && patterns != null) {
      Skript.error("Custom expressions with inline patterns may not have a patterns section.");
      return false;
    }

    String userReturnType = node.getValue("return type");
    if (userReturnType != null) {
      Class returnType =
          Classes.getClassFromUserInput(ScriptLoader.replaceOptions(userReturnType));
      whichInfo.forEach(which -> returnTypes.put(which, returnType));
    }

    ScriptLoader.setCurrentEvent("custom expression getter", ExpressionGetEvent.class);
    SkriptUtil.getItemsFromNode(node, "get").ifPresent(items ->
        whichInfo.forEach(which ->
            expressionHandlers.put(which,
                new Trigger(ScriptLoader.currentScript.getFile(), "get " + which.getPattern(), this, items))
        )
    );

    SyntaxParseEvent.register(this, node, whichInfo, parserHandlers);

    Arrays.stream(Changer.ChangeMode.values())
        .forEach(mode -> {
          String name = mode.name().replace("_", " ").toLowerCase();
          ScriptLoader.setCurrentEvent("custom expression changer", ExpressionChangeEvent.class);
          SkriptUtil.getItemsFromNode(node, name).ifPresent(items ->
              whichInfo.forEach(which -> {
                    Map<Changer.ChangeMode, Trigger> changerMap =
                        changerHandlers.computeIfAbsent(which, k -> new HashMap<>());
                    changerMap.put(mode,
                        new Trigger(ScriptLoader.currentScript.getFile(),
                            String.format("%s %s", name, which.getPattern()), this, items));
                  }
              )
          );
        });

    return true;
  }

  public static SyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }
}

