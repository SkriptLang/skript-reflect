package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.FunctionWrapper;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import com.btk5h.skriptmirror.skript.Consent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class ExprProxy extends SimpleExpression<Object> {
  static {
    Skript.registerExpression(ExprProxy.class, Object.class, ExpressionType.COMBINED,
        "[a] [new] proxy [instance] of %javatypes% (using|from) %objects%");
  }

  private Expression<JavaType> interfaces;
  private Variable<?> handler;

  @Override
  protected Object[] get(Event e) {
    Map<String, FunctionWrapper> handlers = new HashMap<>();
    handler.variablesIterator(e)
        .forEachRemaining(pair -> {
          Object value = pair.getValue();
          if (value instanceof FunctionWrapper) {
            handlers.put(pair.getKey(), ((FunctionWrapper) value));
          }
        });
    return new Object[]{
        Proxy.newProxyInstance(
            LibraryLoader.getClassLoader(),
            Arrays.stream(interfaces.getArray(e))
                .map(JavaType::getJavaClass)
                .filter(Class::isInterface)
                .toArray(Class[]::new),
            new VariableInvocationHandler(handlers)
        )
    };
  }

  private static class VariableInvocationHandler implements InvocationHandler {
    private final Map<String, FunctionWrapper> handlers;

    public VariableInvocationHandler(Map<String, FunctionWrapper> handlers) {
      this.handlers = handlers;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] methodArgs) throws Throwable {
      FunctionWrapper functionWrapper = handlers.get(method.getName().toLowerCase());

      if (functionWrapper == null) {
        return null;
      }

      Function<?> function = functionWrapper.getFunction();
      Object[] functionArgs = functionWrapper.getArguments();

      if (function == null) {
        return null;
      }

      if (methodArgs == null) {
        methodArgs = new Object[0];
      }

      List<Object[]> params = new ArrayList<>(functionArgs.length + methodArgs.length + 1);
      Arrays.stream(functionArgs)
          .map(arg -> new Object[]{arg})
          .forEach(params::add);
      params.add(new Object[]{proxy});
      Arrays.stream(methodArgs)
          .map(arg -> new Object[]{arg})
          .forEach(params::add);

      FunctionEvent functionEvent = new FunctionEvent();

      return function.execute(
          functionEvent,
          params.stream()
              .limit(SkriptReflection.getParameters(function).length)
              .toArray(Object[][]::new)
      );
    }
  }

  @Override
  public boolean isSingle() {
    return true;
  }

  @Override
  public Class<?> getReturnType() {
    return Object.class;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return String.format("proxy of %s from %s",
        interfaces.toString(e, debug),
        handler.toString(e, debug));
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    if (!Consent.Feature.PROXIES.hasConsent(SkriptUtil.getCurrentScript())) {
      return false;
    }

    interfaces = SkriptUtil.defendExpression(exprs[0]);
    Expression<?> var = SkriptUtil.defendExpression(exprs[1]);

    if (var instanceof Variable && ((Variable) var).isList()) {
      handler = ((Variable) var);
      return true;
    }

    Skript.error(var.toString() + " is not a list variable.");
    return false;
  }
}
