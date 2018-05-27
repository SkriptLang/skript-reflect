package com.btk5h.skriptmirror.util;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;

import java.io.File;
import java.util.*;

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
}
