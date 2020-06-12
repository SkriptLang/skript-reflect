package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.function.*;
import ch.njol.skript.registrations.Classes;
import org.eclipse.jdt.annotation.Nullable;

public class FunctionWrapper {

  private static final Function<?> NO_OP_FUNCTION;

  static {
    NO_OP_FUNCTION = new JavaFunction<Object>("$noop", new Parameter[0], Classes.getExactClassInfo(Object.class),
      true) {
      @Nullable
      @Override
      public Object[] execute(FunctionEvent e, Object[][] params) {
        return null;
      }
    };
  }

  private final String name;
  private final Object[] arguments;

  public FunctionWrapper(String name, Object[] arguments) {
    this.name = name;
    this.arguments = arguments;
  }

  public String getName() {
    return name;
  }

  public Object[] getArguments() {
    return arguments;
  }

  public Function<?> getFunction() {
    Function<?> function = Functions.getFunction(name);
    if (function == null) {
      Skript.warning(String.format("The function '%s' could not be resolved.", name));
      return NO_OP_FUNCTION;
    }
    return function;
  }

}

