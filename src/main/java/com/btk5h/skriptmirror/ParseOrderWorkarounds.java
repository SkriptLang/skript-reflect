package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.EffReturn;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.SyntaxElementInfo;
import com.btk5h.skriptmirror.skript.EffExpressionStatement;
import com.btk5h.skriptmirror.skript.custom.ExprMatchedPattern;
import com.btk5h.skriptmirror.skript.custom.condition.CustomCondition;
import com.btk5h.skriptmirror.skript.custom.effect.CustomEffect;
import com.btk5h.skriptmirror.skript.custom.expression.CustomExpression;
import com.btk5h.skriptmirror.util.SkriptReflection;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Explicitly declares the relative parse orders of different statement types. Classes at the start of the list should
 * be parsed before classes at the end of the list.
 * <p>
 * This class should only be used to guarantee that skript-mirror's syntax is parsed before other addons. It cannot
 * guarantee that another addon's syntax will be parsed before skript-reflect.
 */
public class ParseOrderWorkarounds {
  private static final String[] PARSE_ORDER = {
    EffExpressionStatement.class.getCanonicalName(),
    CustomEffect.class.getCanonicalName(),
    CustomCondition.class.getCanonicalName(),
    CustomExpression.class.getCanonicalName(),
    "com.w00tmast3r.skquery.elements.conditions.CondBoolean",
    "com.pie.tlatoani.Miscellaneous.CondBoolean",
    "us.tlatoani.tablisknu.core.base.CondBoolean",
    "com.pie.tlatoani.CustomEvent.EvtCustomEvent",
    EffReturn.class.getCanonicalName(),
    ExprMatchedPattern.class.getCanonicalName(),
    "ch.njol.skript.effects.EffContinue"
  };

  public static void reorderSyntax() {
    Arrays.stream(PARSE_ORDER)
      .forEach(c -> {
        ensureLast(Skript.getStatements(), c);
        ensureLast(Skript.getConditions(), c);
        ensureLast(Skript.getEffects(), c);
        ensureLastExpression(c);
      });
  }

  private static <E extends SyntaxElement> void ensureLast(Collection<SyntaxElementInfo<? extends E>> elements,
                                                           String element) {
    Optional<SyntaxElementInfo<? extends E>> optionalElementInfo = elements.stream()
      .filter(info -> info.c.getName().equals(element))
      .findFirst();

    optionalElementInfo.ifPresent(elementInfo -> {
      elements.remove(elementInfo);
      elements.add(elementInfo);
    });
  }

  private static void ensureLastExpression(String element) {
    List<ExpressionInfo<?, ?>> elements = SkriptReflection.getExpressions();
    if (elements == null)
      return;

    Optional<ExpressionInfo<?, ?>> optionalExpressionInfo = elements.stream()
      .filter(info -> info.c.getName().equals(element))
      .findFirst();

    optionalExpressionInfo.ifPresent(elementInfo -> {
      elements.remove(elementInfo);
      elements.add(elementInfo);
    });
  }

}
