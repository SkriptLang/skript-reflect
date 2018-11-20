package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.function.Parameter;
import ch.njol.skript.registrations.Classes;

public class FunctionWrapper {
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

  public Function getFunction() {
    Function<?> function = Functions.getFunction(name);
    if (function == null) {
      Skript.warning(String.format("The function '%s' could not be resolved.", name));
      return NoOpFunction.INSTANCE;
    }
    return function;
  }

  private static class NoOpFunction extends Function<Object> {
    private static NoOpFunction INSTANCE = new NoOpFunction();

    private NoOpFunction() {
      super("$noop", new Parameter[0], Classes.getExactClassInfo(Object.class), true);
    }

    @Override
    public Object[] execute(FunctionEvent e, Object[][] params) {
      return null;
    }

    @Override
    public boolean resetReturnValue() {
      return false;
    }
  }
}

