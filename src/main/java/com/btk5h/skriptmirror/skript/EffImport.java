package com.btk5h.skriptmirror.skript;

import com.btk5h.skriptmirror.JavaType;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;

public class EffImport extends Effect {
  static {
    Skript.registerEffect(EffImport.class,
        "import %string% [(as|to) [[the] var[iable]] %-object%]");
  }

  private Expression<String> className;
  private Variable var;

  @Override
  protected void execute(Event e) {
    String cls = className.getSingle(e);

    if (cls == null) {
      return;
    }

    JavaType javaType;
    try {
      javaType = new JavaType(Class.forName(cls));
    } catch (ClassNotFoundException ex) {
      Skript.warning(cls + " refers to a non-existent class.");
      return;
    }

    if (var == null) {
      Variables.setVariable(javaType.getJavaClass().getSimpleName().toLowerCase(), javaType, e,
          false);
    } else {
      var.change(e, new Object[]{javaType}, Changer.ChangeMode.SET);
    }
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "import";
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    className = (Expression<String>) exprs[0];

    if (exprs[1] != null) {
      if (!(exprs[1] instanceof Variable)) {
        return false;
      }

      var = (Variable) exprs[1];
    }

    return true;
  }
}
