package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.SyntaxElementInfo;
import com.btk5h.skriptmirror.skript.EffExpressionStatement;
import com.btk5h.skriptmirror.skript.custom.condition.CustomCondition;
import com.btk5h.skriptmirror.skript.custom.effect.CustomEffect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Explicitly declares the relative parse orders of different statement types. Classes at the start of the list should
 * be parsed before classes at the end of the list.
 * <p>
 * This class should only be used to guarantee that skript-mirror's syntax is parsed before other addons. It cannot
 * guarantee that another addon's syntax will be parsed before skript-mirror.
 */
public class ParseOrderWorkarounds {
  private static String[] PARSE_ORDER = {
    EffExpressionStatement.class.getCanonicalName(),
    CustomEffect.class.getCanonicalName(),
    CustomCondition.class.getCanonicalName(),
    "com.w00tmast3r.skquery.elements.conditions.CondBoolean",
    "com.pie.tlatoani.Miscellaneous.CondBoolean"
  };

  public static void reorderSyntax() {
    Arrays.stream(PARSE_ORDER)
      .forEach(c -> {
        ensureLast(Skript.getStatements(), c);
        ensureLast(Skript.getConditions(), c);
        ensureLast(Skript.getEffects(), c);
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
}
