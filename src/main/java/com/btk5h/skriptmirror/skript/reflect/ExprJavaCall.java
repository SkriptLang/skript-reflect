package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.btk5h.skriptmirror.*;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import com.btk5h.skriptmirror.util.StringSimilarity;
import org.bukkit.event.Event;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExprJavaCall<T> implements Expression<T> {
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
  private static final Object[] NO_ARGS = new Object[0];
  private static final Descriptor CONSTRUCTOR_DESCRIPTOR = new Descriptor(null, "<init>", null);

  /**
   * A regular expression that captures potential descriptors without actually validating the descriptor. This is done
   * both for performance reasons and to provide more helpful error messages when using a malformed descriptor.
   */
  private static final String LITE_DESCRIPTOR = "[^0-9.][^.]*\\b";

  static {
    //noinspection unchecked
    Skript.registerExpression(ExprJavaCall.class, Object.class,
        ExpressionType.PATTERN_MATCHES_EVERYTHING,
        "[(2¦try)] %object%..%string%[\\((1¦[%-objects%])\\)]",
        "[(2¦try)] %object%.<" + LITE_DESCRIPTOR + ">[\\((1¦[%-objects%])\\)])",
        "[(2¦try)] [a] new %javatype%\\([%-objects%]\\)");
  }

  private enum CallType {
    FIELD, METHOD, CONSTRUCTOR;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  static Throwable lastError;

  private final LRUCache<Descriptor, Collection<MethodHandle>> callSiteCache = new LRUCache<>(8);

  private File script;
  private boolean suppressErrors;
  private CallType type;

  private Descriptor staticDescriptor;
  private Expression<String> dynamicDescriptor;

  private Expression<Object> rawTarget;
  private Expression<Object> rawArgs;

  private final ExprJavaCall<?> source;
  private final Class<? extends T>[] types;
  private final Class<T> superType;

  @SuppressWarnings("unchecked")
  public ExprJavaCall() {
    this(null, (Class<? extends T>) Object.class);
  }

  @SuppressWarnings("unchecked")
  @SafeVarargs
  private ExprJavaCall(ExprJavaCall<?> source, Class<? extends T>... types) {
    this.source = source;

    if (source != null) {
      this.script = source.script;
      this.suppressErrors = source.suppressErrors;
      this.type = source.type;
      this.staticDescriptor = source.staticDescriptor;
      this.dynamicDescriptor = source.dynamicDescriptor;
      this.rawTarget = source.rawTarget;
      this.rawArgs = source.rawArgs;
    }

    this.types = types;
    this.superType = (Class<T>) Utils.getSuperType(types);
  }

  @Override
  public T getSingle(Event e) {
    Object target = ObjectWrapper.unwrapIfNecessary(rawTarget.getSingle(e));
    Object[] arguments;

    if (target == null) {
      return null;
    }

    if (rawArgs != null) {
      if (rawArgs instanceof ExpressionList && rawArgs.getAnd()) {
        // In a 'comma/and' separated list, manually unwrap each expression and convert nulls to Null wrappers
        // This ensures that expressions that return null do not change the arity of the invoked method
        arguments = Arrays.stream(((ExpressionList<Object>) rawArgs).getExpressions())
            .map(SkriptUtil.unwrapWithEvent(e))
            .map(SkriptMirrorUtil::reifyIfNull)
            .toArray(Object[]::new);
      } else if (rawArgs.isSingle()) {
        // A special case of the above, since a single argument will not be wrapped in a list
        // Directly wrap the argument in an array to ensure the unary method is invoked
        arguments = new Object[]{SkriptMirrorUtil.reifyIfNull(rawArgs.getSingle(e))};
      } else {
        // If the user is using a non-single non-list expression, assume the number of arguments is correct
        arguments = rawArgs.getArray(e);
      }
    } else {
      arguments = NO_ARGS;
    }

    return invoke(target, arguments, getDescriptor(e));
  }

  @Override
  public T[] getArray(Event e) {
    T returnValue = getSingle(e);

    if (returnValue == null) {
      return JavaUtil.newArray(superType, 0);
    }

    T[] arr = JavaUtil.newArray(superType, 1);
    arr[0] = returnValue;

    return arr;
  }

  @Override
  public T[] getAll(Event e) {
    return getArray(e);
  }

  @Override
  public boolean isSingle() {
    return true;
  }

  @Override
  public boolean check(Event e, Checker<? super T> c, boolean negated) {
    return SimpleExpression.check(getAll(e), c, negated, getAnd());
  }

  @Override
  public boolean check(Event e, Checker<? super T> c) {
    return SimpleExpression.check(getAll(e), c, false, getAnd());
  }

  @SafeVarargs
  @Override
  public final <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
    return new ExprJavaCall<>(this, to);
  }

  @Override
  public Class<? extends T> getReturnType() {
    return superType;
  }

  @Override
  public boolean getAnd() {
    return true;
  }

  @Override
  public boolean setTime(int time) {
    return false;
  }

  @Override
  public int getTime() {
    return 0;
  }

  @Override
  public boolean isDefault() {
    return false;
  }

  @Override
  public Iterator<? extends T> iterator(Event e) {
    return new ArrayIterator<>(getAll(e));
  }

  @Override
  public boolean isLoopOf(String s) {
    return false;
  }

  @Override
  public Expression<?> getSource() {
    return source == null ? this : source;
  }

  @Override
  public Expression<? extends T> simplify() {
    return this;
  }

  @Override
  public Class<?>[] acceptChange(Changer.ChangeMode mode) {
    if (type == CallType.FIELD &&
        (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE)) {
      return new Class<?>[]{Object.class};
    }
    return null;
  }

  @Override
  public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
    Object target = ObjectWrapper.unwrapIfNecessary(rawTarget.getSingle(e));

    if (target == null) {
      return;
    }

    Object[] args = new Object[1];

    switch (mode) {
      case SET:
        args[0] = delta[0];
        break;
      case DELETE:
        args[0] = Null.getInstance();
        break;
    }

    invoke(target, args, getDescriptor(e));
  }

  @Override
  public String toString(Event e, boolean debug) {
    switch (type) {
      case FIELD:
        return "" + rawTarget.toString(e, debug) + "." + staticDescriptor.getName();
      case METHOD:
        return "" + rawTarget.toString(e, debug) + "." + staticDescriptor.getName() + "(" +
          (rawArgs == null ? "" : rawArgs.toString(e, debug)) + ")";
      case CONSTRUCTOR:
        return "new " + rawTarget.toString(e, debug) + "(" +  (rawArgs == null ? "" : rawArgs.toString(e, debug)) + ")";
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {

    script = SkriptUtil.getCurrentScript();
    suppressErrors = (parseResult.mark & 2) == 2;

    rawTarget = SkriptUtil.defendExpression(exprs[0]);
    rawArgs = SkriptUtil.defendExpression(exprs[matchedPattern == 0 ? 2 : 1]);

    if (!SkriptUtil.canInitSafely(rawTarget, rawArgs)) {
      return false;
    }

    switch (matchedPattern) {
      case 0:
        type = (parseResult.mark & 1) == 1 ? CallType.METHOD : CallType.FIELD;
        dynamicDescriptor = (Expression<String>) exprs[1];
        break;
      case 1:
        type = (parseResult.mark & 1) == 1 ? CallType.METHOD : CallType.FIELD;
        String desc = parseResult.regexes.get(0).group();

        try {
          staticDescriptor = Descriptor.parse(desc, script);

          if (staticDescriptor == null) {
            Skript.error(desc + " is not a valid descriptor.");
            return false;
          }

          if (staticDescriptor.getJavaClass() != null
              && getCallSite(staticDescriptor).size() == 0) {
            Skript.error(desc + " refers to a non-existent method/field.");
            return false;
          }
        } catch (ClassNotFoundException e) {
          Skript.error(desc + " refers to a non-existent class.");
          return false;
        }
        break;
      case 2:
        type = CallType.CONSTRUCTOR;
        staticDescriptor = CONSTRUCTOR_DESCRIPTOR;
        break;
    }
    return true;
  }

  private void error(Throwable error, String message) {
    lastError = error;

    directError(message);
  }

  private void error(String message) {
    lastError = new JavaCallException(message);

    directError(message);
  }

  private void directError(String message) {
    if (!suppressErrors) {
      Skript.warning(message);
    }
  }

  private boolean hasDynamicDescriptor() {
    return staticDescriptor == null;
  }

  private synchronized Collection<MethodHandle> getCallSite(Descriptor e) {
    return callSiteCache.computeIfAbsent(e, this::createCallSite);
  }

  private Collection<MethodHandle> createCallSite(Descriptor e) {
    Class<?> javaClass = e.getJavaClass();

    switch (type) {
      case FIELD:
        ArrayList<MethodHandle> methodHandles = new ArrayList<>();

        JavaUtil.fields(javaClass)
          .filter(f -> f.getName().equals(e.getName()))
          .peek(f -> f.setAccessible(true))
          .forEach(field -> {
            try {
              methodHandles.add(LOOKUP.unreflectGetter(field));
            } catch (IllegalAccessException ex) {
              Skript.warning(
                String.format("skript-reflect encountered a %s: %s%n" +
                    "Run Skript with the verbosity 'very high' for the stack trace.",
                  ex.getClass().getSimpleName(), ex.getMessage()));

              if (Skript.logVeryHigh()) {
                StringWriter errors = new StringWriter();
                ex.printStackTrace(new PrintWriter(errors));
                Skript.warning(errors.toString());
              }
            }

            try {
              methodHandles.add(LOOKUP.unreflectSetter(field));
            } catch (IllegalAccessException ignored) { }
          });

        return methodHandles.stream()
          .filter(Objects::nonNull)
          .limit(2)
          .collect(Collectors.toList());
      case METHOD:
        Stream<Method> methodStream = JavaUtil.methods(javaClass)
            .filter(m -> m.getName().equals(e.getName()));

        if (e.getParameterTypes() != null) {
          methodStream = methodStream.filter(m -> Arrays.equals(m.getParameterTypes(), e.getParameterTypes()));
        }

        return methodStream
            .peek(m -> m.setAccessible(true))
            .map(JavaUtil.propagateErrors(LOOKUP::unreflect))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
      case CONSTRUCTOR:
        return JavaUtil.constructors(javaClass)
            .peek(c -> c.setAccessible(true))
            .map(JavaUtil.propagateErrors(LOOKUP::unreflectConstructor))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
      default:
        throw new IllegalStateException();
    }
  }

  @SuppressWarnings("unchecked")
  private T invoke(Object target, Object[] arguments, Descriptor baseDescriptor) {
    if (baseDescriptor == null) {
      return null;
    }

    T returnedValue = null;
    Class<?> targetClass = SkriptMirrorUtil.toClassUnwrapJavaTypes(target);
    Descriptor descriptor = baseDescriptor.orDefaultClass(targetClass);

    // If a declaring class is explicitly written, check if the target is a subclass
    if (!descriptor.getJavaClass().isAssignableFrom(targetClass)) {
      error(String.format("Incompatible %s call: %s on %s",
          type, descriptor, SkriptMirrorUtil.getDebugName(targetClass)));
      return null;
    }

    // Copy arguments so that the original array isn't modified by type conversions
    // For instance methods, the target of the call must be added to the start of the arguments array
    Object[] argumentsCopy;
    boolean isStatic = target instanceof JavaType;
    if (isStatic) {
      argumentsCopy = createStaticArgumentsCopy(arguments);
    } else {
      argumentsCopy = createInstanceArgumentsCopy(target, arguments);
    }

    Optional<MethodHandle> method = findCompatibleMethod(descriptor, argumentsCopy);

    if (!method.isPresent()) {
      error(String.format("No matching %s: %s%s",
          type, descriptor.toString(isStatic), argumentsMessage(arguments)));

      suggestParameters(descriptor);
      suggestTypo(descriptor);

      return null;
    }

    MethodHandle mh = method.get();

    argumentsCopy = convertTypes(mh, argumentsCopy);

    try {
      returnedValue = (T) mh.invokeWithArguments(argumentsCopy);
    } catch (Throwable throwable) {
      error(throwable, String.format("%s %s%s threw a %s: %s%n",
        type, descriptor, argumentsMessage(arguments),
        throwable.getClass().getSimpleName(), throwable.getMessage()));
    }

    if (returnedValue == null) {
      return null;
    }

    // Wrap the return value if it isn't recognized by Skript
    if (superType == Object.class || superType == ObjectWrapper.class) {
      returnedValue = (T) ObjectWrapper.wrapIfNecessary(returnedValue, superType == ObjectWrapper.class);
    }

    T converted = Converters.convert(returnedValue, types);

    if (converted == null) {
      String toClasses = Arrays.stream(types)
          .map(SkriptMirrorUtil::getDebugName)
          .collect(Collectors.joining(", "));
      error(String.format("%s %s%s returned %s, which could not be converted to %s",
          type, descriptor, argumentsMessage(arguments), argumentsToString(returnedValue), toClasses));
      return null;
    }

    lastError = null;

    return converted;
  }

  private Descriptor getDescriptor(Event e) {
    if (hasDynamicDescriptor()) {
      String desc = dynamicDescriptor.getSingle(e);

      if (desc == null) {
        error(String.format("Dynamic descriptor %s returned null", dynamicDescriptor.toString(e, false)));
        return null;
      }

      try {
        Descriptor parsedDescriptor = Descriptor.parse(desc, script);

        if (parsedDescriptor == null) {
          error(String.format("Invalid dynamic descriptor %s (%s)", dynamicDescriptor.toString(e, false), desc));
          return null;
        }

        return parsedDescriptor;
      } catch (ClassNotFoundException ex) {
        error(ex, String.format("Class could not be found while parsing the dynamic descriptor %s (%s)",
            dynamicDescriptor.toString(e, false), desc));
        return null;
      }
    }

    return staticDescriptor;
  }

  private static Object[] createStaticArgumentsCopy(Object[] args) {
    return Arrays.copyOf(args, args.length);
  }

  private static Object[] createInstanceArgumentsCopy(Object target, Object[] arguments) {
    Object[] copy = new Object[arguments.length + 1];
    copy[0] = target;
    System.arraycopy(arguments, 0, copy, 1, arguments.length);
    return copy;
  }

  private Optional<MethodHandle> findCompatibleMethod(Descriptor descriptor, Object[] args) {
    return getCallSite(descriptor).stream()
        .filter(mh -> matchesArgs(args, mh))
        .findFirst();
  }

  private static boolean matchesArgs(Object[] args, MethodHandle mh) {
    MethodType mt = mh.type();
    Class<?>[] params = mt.parameterArray();
    int varargsIndex = params.length - 1;
    boolean hasVarargs = mh.isVarargsCollector();

    // Fail early if there is an arity mismatch
    // If the method has varargs, make sure args has the minimum arity (exclude the varargs parameter)
    if (args.length != params.length
        && !(hasVarargs && args.length >= varargsIndex)) {
      return false;
    }

    for (int i = 0; i < args.length; i++) {
      Class<?> param;
      boolean loopAtVarargs = hasVarargs && i >= varargsIndex;

      if (loopAtVarargs) {
        param = params[varargsIndex].getComponentType();
      } else {
        param = params[i];
      }

      Object arg = ObjectWrapper.unwrapIfNecessary(args[i]);

      if (!canCoerceType(arg, param)) {
        // allow varargs arrays to be spread
        if (loopAtVarargs && args.length == params.length && canCoerceType(arg, params[i])) {
          continue;
        }

        return false;
      }
    }

    return true;
  }

  private static boolean canCoerceType(Object o, Class<?> to) {
    if (to.isInstance(o)) {
      return true;
    }

    // coerce numeric types
    if (o instanceof Number && JavaUtil.NUMERIC_CLASSES.contains(to)) {
      return true;
    }

    // coerce arrays of numeric types
    if (to.isArray() && JavaUtil.getArrayDepth(to) == JavaUtil.getArrayDepth(o.getClass())) {
      Class<?> paramComponent = JavaUtil.getBaseComponent(to);
      Class<?> argComponent = JavaUtil.getBaseComponent(o.getClass());

      if (JavaUtil.isNumericClass(paramComponent) && JavaUtil.isNumericClass(argComponent)) {
        return true;
      }
    }

    // allow boxed numbers
    if (to.isPrimitive() && JavaUtil.WRAPPER_CLASSES.get(to).isInstance(o)) {
      return true;
    }

    // coerce single character strings to chars
    if (o instanceof String
        && (to == char.class || to == Character.class)
        && ((String) o).length() == 1) {
      return true;
    }

    // coerce javaclasses and classinfos into classes
    if (to == Class.class && (o instanceof JavaType || o instanceof ClassInfo)) {
      return true;
    }

    // unwrap null wrapper
    return !to.isPrimitive() && o instanceof Null;
  }

  private static Object[] convertTypes(MethodHandle mh, Object[] args) {
    Class<?>[] params = mh.type().parameterArray();
    int varargsIndex = params.length - 1;
    boolean hasVarargs = mh.isVarargsCollector();

    for (int i = 0; i < args.length; i++) {
      Class<?> param;
      boolean loopAtVarargs = hasVarargs && i >= varargsIndex;

      // varargs parameters are always arrays, but the method handle expects the array to be spread before called
      if (loopAtVarargs) {
        param = params[varargsIndex].getComponentType();
      } else {
        param = params[i];
      }

      args[i] = ObjectWrapper.unwrapIfNecessary(args[i]);

      // spread varargs arrays
      if (loopAtVarargs && args.length == params.length && params[i].isInstance(args[i])) {
        Object varargsArray = args[i];
        int varargsLength = Array.getLength(varargsArray);

        args = Arrays.copyOf(args, args.length - 1 + varargsLength);

        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(varargsArray, 0, args, varargsIndex, varargsLength);
      }

      // coerce numeric types
      if (param.isPrimitive() && args[i] instanceof Number) {
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

      // coerce arrays of numeric types
      if (param.isArray()
          && JavaUtil.getArrayDepth(param) == JavaUtil.getArrayDepth(args[i].getClass())
          && JavaUtil.isNumericClass(JavaUtil.getBaseComponent(param))) {
        args[i] = JavaUtil.convertNumericArray(args[i], JavaUtil.getBaseComponent(param));
      }

      // coerce single character strings to chars
      if (args[i] instanceof String
          && (param == char.class || param == Character.class)) {
        args[i] = ((String) args[i]).charAt(0);
      }

      // coerce javatypes and classinfos into classes
      if (param == Class.class) {
        if (args[i] instanceof JavaType) {
          args[i] = ((JavaType) args[i]).getJavaClass();
        } else if (args[i] instanceof ClassInfo) {
          args[i] = ((ClassInfo<?>) args[i]).getC();
        }
      }

      // unwrap null wrapper
      if (args[i] instanceof Null) {
        args[i] = null;
      }
    }

    return args;
  }

  private void suggestParameters(Descriptor descriptor) {
    if (!(type == CallType.CONSTRUCTOR || type == CallType.METHOD)) {
      return;
    }

    String guess = descriptor.getName();
    Class<?> javaClass = descriptor.getJavaClass();

    Stream<? extends Executable> members = getExecutables(javaClass);

    List<String> matches = members
        .filter(e -> e.getName().equals(guess))
        .map(Executable::getParameters)
        .map(params ->
            Arrays.stream(params)
                .map(Parameter::getType)
                .map(Class::getTypeName)
                .collect(Collectors.joining(","))
        )
        .collect(Collectors.toList());

    if (!matches.isEmpty()) {
      directError(String.format("Did you pass the wrong parameters? Here are the parameter signatures for %s:", guess));
      matches.forEach(parameterList -> directError(String.format("* %s(%s)", guess, parameterList)));
    }
  }

  private void suggestTypo(Descriptor descriptor) {
    String guess = descriptor.getName();
    Class<?> javaClass = descriptor.getJavaClass();

    Stream<? extends Member> members = getMembers(javaClass);

    List<String> matches = members
        .map(Member::getName)
        .filter(m -> !m.equals(guess)) // this would be a parameter mismatch, not a typo
        .distinct()
        .map(m -> StringSimilarity.compare(guess, m, 3))
        .filter(Objects::nonNull)
        .sorted()
        .map(StringSimilarity.Result::getRight)
        .collect(Collectors.toList());

    if (!matches.isEmpty()) {
      directError(String.format("Did you misspell the %s? You may have meant to type one of the following:", type));
      matches.forEach(name -> directError("* " + name));
    }
  }

  private Stream<? extends Executable> getExecutables(Class<?> javaClass) {
    switch (type) {
      case METHOD:
        return JavaUtil.methods(javaClass);
      case CONSTRUCTOR:
        return JavaUtil.constructors(javaClass);
      default:
        throw new IllegalStateException();
    }
  }

  private Stream<? extends Member> getMembers(Class<?> javaClass) {
    switch (type) {
      case FIELD:
        return JavaUtil.fields(javaClass);
      case METHOD:
        return JavaUtil.methods(javaClass);
      case CONSTRUCTOR:
        return JavaUtil.constructors(javaClass);
      default:
        throw new IllegalStateException();
    }
  }

  private String argumentsMessage(Object... arguments) {
    if (type == CallType.FIELD) {
      return "";
    }

    if (arguments.length == 0) {
      return " called without arguments";
    }

    return " called with (" + argumentsToString(arguments) + ")";
  }

  private static String argumentsToString(Object... arguments) {
    return Arrays.stream(arguments)
        .map(arg -> String.format("%s (%s)",
            Classes.toString(arg), SkriptMirrorUtil.getDebugName(SkriptMirrorUtil.getClass(arg))))
        .collect(Collectors.joining(", "));
  }
}
