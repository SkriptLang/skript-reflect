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

}
