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

  public static Optional<List<TriggerItem>> getItemsFromNode(SectionNode node, String key) {
    Node subNode = node.get(key);
    if (!(subNode instanceof SectionNode)) {
      return Optional.empty();
    }

    RetainingLogHandler log = SkriptLogger.startRetainingLog();
    try {
      return Optional.of(ScriptLoader.loadItems(((SectionNode) subNode)));
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

  public static String replaceUserInputPatterns(String part) {
    NonNullPair<String, Boolean> info = Utils.getEnglishPlural(part);

    ClassInfo<?> ci = Classes.getClassInfoNoError(info.getFirst());

    if (ci == null) {
      ci = Classes.getClassInfoFromUserInput(info.getFirst());
    }

    if (ci == null) {
      Skript.warning(String.format("'%s' is not a valid Skript type. Using 'object' instead.", part));
      return info.getSecond() ? "objects" : "object";
    }

    return Utils.toEnglishPlural(ci.getCodeName(), info.getSecond());
  }
}
