package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.EffReturn;
import ch.njol.util.Checker;
import org.skriptlang.reflect.syntax.condition.elements.CustomCondition;
import org.skriptlang.reflect.syntax.effect.elements.CustomEffect;
import org.skriptlang.reflect.syntax.expression.elements.CustomExpression;
import com.btk5h.skriptmirror.skript.EffExpressionStatement;
import com.btk5h.skriptmirror.skript.custom.ExprMatchedPattern;
import com.btk5h.skriptmirror.util.SkriptReflection;

import java.util.Collection;
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
    "ch.njol.skript.effects.EffContinue",
    "com.ankoki.skjade.elements.conditions.CondBoolean"
  };

  public static void reorderSyntax() {
    for (String c : PARSE_ORDER) {
      ensureLast(Skript.getStatements(), o -> o.getElementClass().getName().equals(c));
      ensureLast(Skript.getConditions(), o -> o.getElementClass().getName().equals(c));
      ensureLast(Skript.getEffects(), o -> o.getElementClass().toString().equals(c));
      ensureLast(SkriptReflection.getExpressions(), o -> o.getElementClass().getName().equals(c));
      ensureLast(Skript.getEvents(), o -> o.getElementClass().getName().equals(c));
    }
  }

  private static <E> void ensureLast(Collection<E> elements, Checker<E> checker) {
    Optional<E> optionalE = elements.stream()
      .filter(checker::check)
      .findFirst();

    optionalE.ifPresent(value -> {
      elements.remove(value);
      elements.add(value);
    });
  }

}
