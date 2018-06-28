package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.SyntaxElementInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class ParseOrderWorkarounds {
  private static String[] PARSE_ORDER = {
      "com.btk5h.skriptmirror.skript.CondExpressionStatement",
      "com.btk5h.skriptmirror.skript.custom.effect.CustomEffect",
      "com.btk5h.skriptmirror.skript.custom.condition.CustomCondition",
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
