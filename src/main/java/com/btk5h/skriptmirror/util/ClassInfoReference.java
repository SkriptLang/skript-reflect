package com.btk5h.skriptmirror.util;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Objects;

public class ClassInfoReference {

  private ClassInfo<?> classInfo;
  private boolean plural;
  private boolean specific;

  /**
   * Creates a non-specific ClassInfoReference
   * @param classInfo the classinfo referenced
   */
  public ClassInfoReference(ClassInfo<?> classInfo) {
    this.classInfo = classInfo;
  }

  /**
   * Creates a specific ClassInfoReference
   * @param classInfo the classinfo referenced
   * @param plural whether the reference to the classinfo is plural
   */
  public ClassInfoReference(ClassInfo<?> classInfo, boolean plural) {
    this.classInfo = classInfo;
    this.plural = plural;
    this.specific = true;
  }

  public ClassInfo<?> getClassInfo() {
    return classInfo;
  }

  public boolean isSpecific() {
    return specific;
  }

  public boolean isPlural() {
    return plural;
  }

  public static Expression<ClassInfoReference> getFromClassInfoExpression(Expression<ClassInfo<?>> expression) {
    ClassInfoReference parsedReference = SkriptUtil.getClassInfoReference(expression);
    return new SimpleExpression<ClassInfoReference>() {

      @Override
      public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return expression.init(expressions, matchedPattern, isDelayed, parseResult);
      }

      @Override
      protected ClassInfoReference[] get(Event event) {
        if (parsedReference != null) {
          return new ClassInfoReference[] { parsedReference };
        } else if (isSingle()) {
          ClassInfo<?> classInfo = expression.getSingle(event);
          if (classInfo == null) {
            return new ClassInfoReference[0];
          }
          return new ClassInfoReference[] { new ClassInfoReference(classInfo) };
        } else {
          return expression.stream(event)
              .filter(Objects::nonNull)
              .map(ClassInfoReference::new)
              .toArray(ClassInfoReference[]::new);
        }
      }

      public Class<? extends ClassInfoReference> getReturnType() {
        return ClassInfoReference.class;
      }

      @Override
      public boolean isSingle() {
        return expression.isSingle();
      }


      @Override
      public String toString(@Nullable Event event, boolean debug) {
        if (debug) {
          return expression.toString(event, true) + " (wrapped by ClassInfoReference)";
        }
        return expression.toString(event, false);
      }

    };
  }

}
