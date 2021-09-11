package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.FunctionWrapper;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.skript.reflect.sections.Section;
import com.btk5h.skriptmirror.skript.reflect.sections.SectionEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExprProxy extends SimpleExpression<Object> {

  public static boolean proxiesUsed = false;

  static {
    Skript.registerExpression(ExprProxy.class, Object.class, ExpressionType.COMBINED,
        "[a] [new] proxy [instance] of %javatypes% (using|from) %objects%");
  }

  private Expression<JavaType> interfaces;
  private Variable<?> handler;

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    proxiesUsed = true;

    interfaces = SkriptUtil.defendExpression(exprs[0]);
    if (interfaces instanceof Literal) {
      JavaType[] javaTypes = ((Literal<JavaType>) interfaces).getArray();
      for (JavaType javaType : javaTypes) {
        if (!javaType.getJavaClass().isInterface()) {
          Skript.warning(javaType + " is not an interface");
        }
      }
    }

    Expression<?> var = SkriptUtil.defendExpression(exprs[1]);
    if (var instanceof Variable && ((Variable<?>) var).isList()) {
      handler = (Variable<?>) var;
      return true;
    }

    Skript.error(var + " is not a list variable.");
    return false;
  }

  @Override
  protected Object[] get(Event e) {
    Map<String, FunctionWrapper> handlers = new HashMap<>();
    Map<String, Section> sectionHandlers = new HashMap<>();
    handler.variablesIterator(e)
        .forEachRemaining(pair -> {
          Object value = pair.getValue();
          if (value instanceof FunctionWrapper) {
            handlers.put(pair.getKey(), (FunctionWrapper) value);
          } else if (value instanceof Section) {
            sectionHandlers.put(pair.getKey(), (Section) value);
          }
        });

    return new Object[]{
        Proxy.newProxyInstance(
            LibraryLoader.getClassLoader(),
            Arrays.stream(interfaces.getArray(e))
                .map(JavaType::getJavaClass)
                .filter(Class::isInterface)
                .toArray(Class[]::new),
            new VariableInvocationHandler(handlers, sectionHandlers)
        )
    };
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

  private static class VariableInvocationHandler implements InvocationHandler {
    private final Map<String, FunctionWrapper> handlers;
    private final Map<String, Section> sectionHandlers;

    public VariableInvocationHandler(Map<String, FunctionWrapper> handlers, Map<String, Section> sectionHandlers) {
      this.handlers = handlers;
      this.sectionHandlers = sectionHandlers;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] methodArgs) {
      FunctionWrapper functionWrapper = handlers.get(method.getName().toLowerCase());
      Section section = sectionHandlers.get(method.getName().toLowerCase());

      if (functionWrapper == null && section == null) {
        return null;
      }

      Function<?> function = functionWrapper == null ? null : functionWrapper.getFunction();
      Object[] functionArgs = functionWrapper == null ? new Object[0] : functionWrapper.getArguments();

      if (functionWrapper != null && function == null) {
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

      Object[] returnValue;
      if (function != null) {
        FunctionEvent<?> functionEvent = new FunctionEvent<>(function);

        Object[][] args = params.stream()
          .limit(function.getParameters().length)
          .toArray(Object[][]::new);

        returnValue = function.execute(functionEvent, args);
      } else {
        SectionEvent sectionEvent = new SectionEvent(null, section);

        section.run(sectionEvent, params.toArray(new Object[0][]));
        returnValue = section.getOutput();
      }

      return (returnValue == null || returnValue.length == 0) ? null : ObjectWrapper.unwrapIfNecessary(returnValue[0]);
    }
  }

}
