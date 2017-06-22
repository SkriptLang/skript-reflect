package com.btk5h.skriptmirror.skript;

import com.btk5h.skriptmirror.Descriptor;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LRUCache;
import com.btk5h.skriptmirror.Util;

import org.bukkit.event.Event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

public class ExprJavaCall extends SimpleExpression<Object> {
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
  private static final Object[] NO_ARGS = new Object[0];
  private static final Descriptor CONSTRUCTOR_DESCRIPTOR = Descriptor.create("<init>");
  private static final Map<Class<?>, Class<?>> WRAPPER_CLASSES = new HashMap<>();
  private static final Set<Class<?>> NUMERIC_CLASSES = new HashSet<>();

  static {
    WRAPPER_CLASSES.put(boolean.class, Boolean.class);
    WRAPPER_CLASSES.put(byte.class, Byte.class);
    WRAPPER_CLASSES.put(char.class, Character.class);
    WRAPPER_CLASSES.put(double.class, Double.class);
    WRAPPER_CLASSES.put(float.class, Float.class);
    WRAPPER_CLASSES.put(int.class, Integer.class);
    WRAPPER_CLASSES.put(long.class, Long.class);
    WRAPPER_CLASSES.put(short.class, Short.class);

    NUMERIC_CLASSES.add(byte.class);
    NUMERIC_CLASSES.add(double.class);
    NUMERIC_CLASSES.add(float.class);
    NUMERIC_CLASSES.add(int.class);
    NUMERIC_CLASSES.add(long.class);
    NUMERIC_CLASSES.add(short.class);
  }

  static {
    Skript.registerExpression(ExprJavaCall.class, Object.class,
        ExpressionType.PATTERN_MATCHES_EVERYTHING,
        "%objects%..%string%(0¦!|1¦\\([%-objects%]\\))",
        "%objects%.<[\\w$.\\[\\]]+>(0¦!|1¦\\([%-objects%]\\))",
        "new %javatype%\\([%-objects%]\\)");
  }


  private enum Type {
    FIELD, METHOD, CONSTRUCTOR
  }

  private LRUCache<Descriptor, Collection<MethodHandle>> callSiteCache = new LRUCache<>(8);

  private Expression<Object> target;
  private Expression<Object> args;

  private Type type;
  private boolean isDynamic;

  private Descriptor staticDescriptor;
  private Expression<String> dynamicDescriptor;

  private Collection<MethodHandle> getCallSite(Descriptor e) {
    return callSiteCache.computeIfAbsent(e, this::createCallSite);
  }

  private Collection<MethodHandle> createCallSite(Descriptor e) {
    Class<?> javaClass = e.getJavaClass();

    switch (type) {
      case FIELD:
        return Util.fields(javaClass)
            .filter(f -> f.getName().equals(e.getIdentifier()))
            .peek(f -> f.setAccessible(true))
            .flatMap(Util.propagateErrors(f -> Stream.of(
                LOOKUP.unreflectGetter(f),
                LOOKUP.unreflectSetter(f)
            )))
            .limit(2)
            .collect(Collectors.toList());
      case METHOD:
        return Util.methods(javaClass)
            .filter(m -> m.getName().equals(e.getIdentifier()))
            .peek(m -> m.setAccessible(true))
            .map(Util.propagateErrors(LOOKUP::unreflect))
            //.map(ExprJavaCall::asSpreader)
            .collect(Collectors.toList());
      case CONSTRUCTOR:
        return Util.constructor(javaClass)
            .peek(c -> c.setAccessible(true))
            .map(Util.propagateErrors(LOOKUP::unreflectConstructor))
            //.map(ExprJavaCall::asSpreader)
            .collect(Collectors.toList());
      default:
        throw new IllegalStateException();
    }
  }

  private static MethodHandle asSpreader(MethodHandle mh) {
    int paramCount = mh.type().parameterCount();
    if (mh.isVarargsCollector()) {
      if (paramCount == 1) {
        return mh;
      }

      return mh.asSpreader(Object[].class, paramCount - 1);
    }

    return mh.asSpreader(Object[].class, paramCount);
  }


  @Override
  protected Object[] get(Event e) {
    Object[] targets = target.getArray(e);
    Object[] arguments;
    if (args != null) {
      try {
        arguments = args.getArray(e);
      } catch (SkriptAPIException ex) {
        Skript.error("The arguments passed to " + getDescriptor(e) + " could not be parsed. Try " +
            "setting a list variable to the arguments and pass that variable to the reflection " +
            "call instead!");
        return null;
      }
    } else {
      arguments = NO_ARGS;
    }

    Descriptor baseDescriptor = getDescriptor(e);

    return invoke(targets, arguments, baseDescriptor);
  }

  private Object[] invoke(Object[] targets, Object[] arguments, Descriptor baseDescriptor) {
    List<Object> returnedValues = new ArrayList<>();

    for (Object obj : targets) {
      Descriptor descriptor;
      Class<?> targetClass = Util.toClass(obj);

      descriptor = specifyDescriptor(baseDescriptor, targetClass);

      if (descriptor.getJavaClass().isAssignableFrom(targetClass)) {
        Object[] arr;
        if (obj instanceof JavaType) {
          arr = new Object[arguments.length];
          System.arraycopy(arguments, 0, arr, 0, arguments.length);
        } else {
          arr = new Object[arguments.length + 1];
          arr[0] = obj;
          System.arraycopy(arguments, 0, arr, 1, arguments.length);
        }

        Class<?>[] argTypes = Arrays.stream(arr)
            .map(Object::getClass)
            .toArray(Class<?>[]::new);

        Optional<MethodHandle> method = selectMethod(descriptor, argTypes);

        if (method.isPresent()) {
          MethodHandle mh = method.get();

          convertTypes(mh.type(), arr);

          try {
            Object value = mh.invokeWithArguments(arr);

            if (mh.type().returnType() != void.class) {
              returnedValues.add(value);
            }
          } catch (Throwable throwable) {
            // TODO error handling
          }
        }
      }
    }

    if (isSingle()) {
      switch (returnedValues.size()) {
        case 1:
          return new Object[]{returnedValues.get(0)};
        case 0:
          return null;
        default:
          return new Object[]{returnedValues.toArray()};
      }
    }

    return returnedValues.toArray();
  }

  private Descriptor getDescriptor(Event e) {
    if (isDynamic) {
      String desc = dynamicDescriptor.getSingle(e);

      if (desc == null) {
        return null;
      }

      try {
        return Descriptor.parse(desc);
      } catch (ClassNotFoundException ignored) {
        // TODO error handling
        return Descriptor.create(null);
      }
    }

    return staticDescriptor;
  }

  private static Descriptor specifyDescriptor(Descriptor descriptor, Class<?> cls) {
    if (descriptor.getJavaClass() != null) {
      return descriptor;
    }

    return Descriptor.create(cls, descriptor.getIdentifier());
  }

  private Optional<MethodHandle> selectMethod(Descriptor descriptor, Class<?>[] argTypes) {
    return getCallSite(descriptor).stream()
        .filter(mh -> matchesArgs(argTypes, mh))
        .findFirst();
  }

  private static boolean matchesArgs(Class<?>[] args, MethodHandle mh) {
    MethodType mt = mh.type();
    if (mt.parameterCount() != args.length && !mh.isVarargsCollector()) {
      return false;
    }

    Class<?>[] params = mt.parameterArray();

    for (int i = 0; i < params.length; i++) {
      if (i == params.length - 1 && mh.isVarargsCollector()) {
        break;
      }

      Class<?> param = params[i];
      Class<?> arg = args[i];

      if (!param.isAssignableFrom(arg)) {
        if (Number.class.isAssignableFrom(arg) && NUMERIC_CLASSES.contains(param)) {
          continue;
        }

        if (param.isPrimitive() && arg == WRAPPER_CLASSES.get(param)) {
          continue;
        }

        return false;
      }
    }

    return true;
  }

  private static void convertTypes(MethodType mt, Object[] args) {
    if (!mt.hasPrimitives()) {
      return;
    }

    Class<?>[] params = mt.parameterArray();

    for (int i = 0; i < params.length; i++) {
      Class<?> param = params[i];

      if (param.isPrimitive()) {
        if (param == byte.class) {
          args[i] = ((Number) args[i]).byteValue();
        } else if (param == double.class) {
          args[i] = ((Number) args[i]).doubleValue();
        } else if (param == float.class) {
          args[i] = ((Number) args[i]).floatValue();
        } else if (param == int.class) {
          args[i] = ((Number) args[i]).intValue();
        } else if (param == long.class) {
          args[i] = ((Number) args[i]).longValue();
        } else if (param == short.class) {
          args[i] = ((Number) args[i]).shortValue();
        }
      }
    }
  }

  @Override
  public boolean isSingle() {
    return type != Type.FIELD || target.isSingle();
  }

  @Override
  public Class<?> getReturnType() {
    return Object.class;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return type + " " + getDescriptor(e);
  }

  @Override
  public Class<?>[] acceptChange(Changer.ChangeMode mode) {
    if (type == Type.FIELD &&
        (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE)) {
      return new Class<?>[]{Object.class};
    }

    return super.acceptChange(mode);
  }

  @Override
  public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
    Object[] args = new Object[1];

    switch (mode) {
      case SET:
        args[0] = delta[0];
        break;
      case DELETE:
        args[0] = null;
        break;
    }

    Descriptor baseDescriptor = getDescriptor(e);

    invoke(target.getArray(e), args, baseDescriptor);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    target = (Expression<Object>) exprs[0];
    args = (Expression<Object>) exprs[matchedPattern == 0 ? 2 : 1];

    if (target instanceof UnparsedLiteral || args instanceof UnparsedLiteral) {
      return false;
    }

    switch (matchedPattern) {
      case 0:
        isDynamic = true;
        type = parseResult.mark == 0 ? Type.FIELD : Type.METHOD;

        dynamicDescriptor = (Expression<String>) exprs[1];
        break;
      case 1:
        isDynamic = false;
        type = parseResult.mark == 0 ? Type.FIELD : Type.METHOD;
        String desc = parseResult.regexes.get(0).group();

        try {
          staticDescriptor = Descriptor.parse(desc);

          if (staticDescriptor == null) {
            Skript.error(desc + " is not a valid descriptor.");
            return false;
          }

          if (staticDescriptor.getJavaClass() != null && getCallSite(staticDescriptor).size() == 0) {
            Skript.error(desc + " refers to a non-existent method/field.");
            return false;
          }
        } catch (ClassNotFoundException e) {
          Skript.error(desc + " refers to a non-existent class.");
          return false;
        }
        break;
      case 2:
        type = Type.CONSTRUCTOR;
        staticDescriptor = CONSTRUCTOR_DESCRIPTOR;
        break;
    }

    return true;
  }
}
