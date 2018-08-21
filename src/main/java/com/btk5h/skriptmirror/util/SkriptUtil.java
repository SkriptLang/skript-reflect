package com.btk5h.skriptmirror.util;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SkriptUtil {
  @SuppressWarnings("unchecked")
  public static <T> Expression<T> defendExpression(Expression<?> expr) {
    if (expr instanceof UnparsedLiteral) {
      Literal<?> parsed = ((UnparsedLiteral) expr).getConvertedExpression(Object.class);
      return (Expression<T>) (parsed == null ? expr : parsed);
    } else if (expr instanceof ExpressionList) {
      Expression[] exprs = ((ExpressionList) expr).getExpressions();
      for (int i = 0; i < exprs.length; i++) {
        exprs[i] = defendExpression(exprs[i]);
      }
    }
    return (Expression<T>) expr;
  }

  public static boolean hasUnparsedLiteral(Expression<?> expr) {
    return expr instanceof UnparsedLiteral ||
        (expr instanceof ExpressionList &&
            Arrays.stream(((ExpressionList) expr).getExpressions())
                .anyMatch(UnparsedLiteral.class::isInstance));
  }

  public static boolean canInitSafely(Expression<?>... expressions) {
    return Arrays.stream(expressions)
        .filter(Objects::nonNull)
        .noneMatch(SkriptUtil::hasUnparsedLiteral);
  }

  public static List<TriggerItem> getItemsFromNode(SectionNode node) {
    RetainingLogHandler log = SkriptLogger.startRetainingLog();
    try {
      return ScriptLoader.loadItems(node);
    } finally {
      SkriptReflection.printLog(log);
      ScriptLoader.deleteCurrentEvent();
    }
  }

  public static void clearSectionNode(SectionNode node) {
    List<Node> subNodes = new ArrayList<>();
    node.forEach(subNodes::add);
    subNodes.forEach(Node::remove);
  }

  public static File getCurrentScript() {
    Config currentScript = ScriptLoader.currentScript;
    return currentScript == null ? null : currentScript.getFile();
  }

  public static ClassInfo<?> getUserClassInfo(String name) {
    NonNullPair<String, Boolean> wordData = Utils.getEnglishPlural(name);

    ClassInfo<?> ci = Classes.getClassInfoNoError(wordData.getFirst());

    if (ci == null) {
      ci = Classes.getClassInfoFromUserInput(wordData.getFirst());
    }

    if (ci == null) {
      Skript.warning(String.format("'%s' is not a valid Skript type. Using 'object' instead.", name));
      return Classes.getExactClassInfo(Object.class);
    }

    return ci;
  }

  public static NonNullPair<ClassInfo<?>, Boolean> getUserClassInfoAndPlural(String name) {
    NonNullPair<String, Boolean> wordData = Utils.getEnglishPlural(name);
    ClassInfo<?> ci = getUserClassInfo(name);

    return new NonNullPair<>(ci, wordData.getSecond());
  }

  public static String replaceUserInputPatterns(String name) {
    NonNullPair<String, Boolean> wordData = Utils.getEnglishPlural(name);
    ClassInfo<?> ci = getUserClassInfo(name);

    return Utils.toEnglishPlural(ci.getCodeName(), wordData.getSecond());
  }
}
