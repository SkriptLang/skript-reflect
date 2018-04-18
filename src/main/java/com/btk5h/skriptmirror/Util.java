package com.btk5h.skriptmirror;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.function.Parameter;
import ch.njol.skript.log.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public final class Util {
  public static final Map<Class<?>, Class<?>> WRAPPER_CLASSES = new HashMap<>();
  public static final Set<Class<?>> NUMERIC_CLASSES = new HashSet<>();
  public static final Map<String, Class<?>> PRIMITIVE_CLASS_NAMES = new HashMap<>();

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

    PRIMITIVE_CLASS_NAMES.put("boolean", boolean.class);
    PRIMITIVE_CLASS_NAMES.put("byte", byte.class);
    PRIMITIVE_CLASS_NAMES.put("char", char.class);
    PRIMITIVE_CLASS_NAMES.put("double", double.class);
    PRIMITIVE_CLASS_NAMES.put("float", float.class);
    PRIMITIVE_CLASS_NAMES.put("int", int.class);
    PRIMITIVE_CLASS_NAMES.put("long", long.class);
    PRIMITIVE_CLASS_NAMES.put("short", short.class);
  }

  public static final String IDENTIFIER = "[_a-zA-Z$][\\w$]*";
  public static final String PACKAGE = "(?:" + IDENTIFIER + "\\.)*(?:" + IDENTIFIER + ")";

  private Util() {
  }

  private static Field PATTERNS;
  private static Field PARAMETERS;
  private static Field HANDLERS;

  static {
    Field _FIELD = null;
    try {
      _FIELD = SyntaxElementInfo.class.getDeclaredField("patterns");
      _FIELD.setAccessible(true);
      PATTERNS = _FIELD;
    } catch (NoSuchFieldException e) {
      Skript.warning("Skript's pattern info field could not be resolved. " +
          "Custom syntax will not work.");
    }

    try {
      _FIELD = ch.njol.skript.lang.function.Function.class.getDeclaredField("parameters");
      _FIELD.setAccessible(true);
      PARAMETERS = _FIELD;
    } catch (NoSuchFieldException e) {
      Skript.warning("Skript's parameters field could not be resolved. " +
          "Class proxies will not work.");
    }

    try {
      _FIELD = SkriptLogger.class.getDeclaredField("handlers");
      _FIELD.setAccessible(true);
      HANDLERS = _FIELD;
    } catch (NoSuchFieldException e) {
      Skript.warning("Skript's handlers field could not be resolved. Some Skript warnings may not be available.");
    }
  }

  public static void setPatterns(SyntaxElementInfo<?> info, String[] patterns) {
    if (PATTERNS != null) {
      try {
        PATTERNS.set(info, patterns);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  public static Parameter<?>[] getParameters(ch.njol.skript.lang.function.Function function) {
    if (PARAMETERS != null) {
      try {
        return ((Parameter<?>[]) PARAMETERS.get(function));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    throw new IllegalStateException();
  }

  public static Stream<Field> fields(Class<?> cls) {
    return Stream.concat(
        Arrays.stream(cls.getFields()),
        Arrays.stream(cls.getDeclaredFields())
            .filter(Util::notPublic)
    );
  }

  public static Stream<Method> methods(Class<?> cls) {
    return Stream.concat(
        Arrays.stream(cls.getMethods()),
        Arrays.stream(cls.getDeclaredMethods())
            .filter(Util::notPublic)
    );
  }

  public static Stream<Constructor> constructor(Class<?> cls) {
    return Arrays.stream(cls.getDeclaredConstructors());
  }

  public static String toGenericString(Member o) {
    if (o instanceof Field) {
      return ((Field) o).toGenericString();
    } else if (o instanceof Method) {
      return ((Method) o).toGenericString();
    } else if (o instanceof Constructor) {
      return ((Constructor) o).toGenericString();
    }
    return null;
  }

  public static boolean notPublic(Member m) {
    return !Modifier.isPublic(m.getModifiers());
  }

  public static String preprocessPattern(String pattern) {
    StringBuilder newPattern = new StringBuilder(pattern.length());
    String[] parts = pattern.split("%");

    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (i % 2 == 0) {
        newPattern.append(part);
      } else {
        if (part.startsWith("_")) {
          part = part.endsWith("s") ? "objects" : "object";
        }

        newPattern.append('%');
        newPattern.append(part);
        newPattern.append('%');
      }
    }

    return newPattern.toString();
  }

  @SuppressWarnings("unchecked")
  public static <T> Expression<T> defendExpression(Expression<?> expr) {
    if (expr instanceof UnparsedLiteral) {
      Literal<?> parsed = ((UnparsedLiteral) expr).getConvertedExpression(Object.class);
      return (Expression<T>) (parsed == null ? expr : parsed);
    } else if (expr instanceof ExpressionList) {
      Expression[] exprs = ((ExpressionList) expr).getExpressions();
      for (int i = 0; i < exprs.length; i++) {
        exprs[i] = defendExpression(exprs[i]);
      }
    }
    return (Expression<T>) expr;
  }

  private static boolean hasUnparsedLiteral(Expression<?> expr) {
    return expr instanceof UnparsedLiteral ||
        (expr instanceof ExpressionList &&
            Arrays.stream(((ExpressionList) expr).getExpressions())
                .anyMatch(UnparsedLiteral.class::isInstance));
  }

  public static boolean canInitSafely(Expression<?>... expressions) {
    return Arrays.stream(expressions)
        .filter(Objects::nonNull)
        .noneMatch(Util::hasUnparsedLiteral);
  }

  public static Optional<List<TriggerItem>> getItemsFromNode(SectionNode node, String key) {
    Node subNode = node.get(key);
    if (!(subNode instanceof SectionNode)) {
      return Optional.empty();
    }

    RetainingLogHandler log = SkriptLogger.startRetainingLog();
    try {
      return Optional.of(ScriptLoader.loadItems(((SectionNode) subNode)));
    } finally {
      printLog(log);
      ScriptLoader.deleteCurrentEvent();
    }
  }

  private static void printLog(RetainingLogHandler logger) {
    logger.stop();
    if (HANDLERS != null) {
      HandlerList handler;
      try {
        handler = (HandlerList) HANDLERS.get(logger);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        return;
      }

      Iterator<LogHandler> handlers = handler.iterator();
      LogHandler nextHandler;
      List<LogHandler> parseLogs = new ArrayList<>();

      while (handlers.hasNext() && (nextHandler = handlers.next()) instanceof ParseLogHandler) {
        parseLogs.add(nextHandler);
      }

      parseLogs.forEach(LogHandler::stop);
      SkriptLogger.logAll(logger.getLog());
    }
  }

  public static Object boxPrimitiveArray(Object obj) {
    Class<?> componentType = obj.getClass().getComponentType();
    if (componentType != null && componentType.isPrimitive()) {
      int length = Array.getLength(obj);
      Object[] boxedArray = newArray(WRAPPER_CLASSES.get(componentType), length);

      for (int i = 0; i < length; i++) {
        boxedArray[i] = Array.get(obj, i);
      }

      obj = boxedArray;
    }
    return obj;
  }

  @FunctionalInterface
  public interface ExceptionalFunction<T, R> {
    R apply(T t) throws Exception;
  }

  @SuppressWarnings("ThrowableNotThrown")
  public static <T, R> Function<T, R> propagateErrors(ExceptionalFunction<T, R> function) {
    return t -> {
      try {
        return function.apply(t);
      } catch (Exception e) {
        Skript.warning(
            String.format("skript-mirror encountered a %s: %s%n" +
                    "Run Skript with the verbosity 'very high' for the stack trace.",
                e.getClass().getSimpleName(), e.getMessage()));

        if (Skript.logVeryHigh()) {
          StringWriter errors = new StringWriter();
          e.printStackTrace(new PrintWriter(errors));
          Skript.warning(errors.toString());
        }
      }
      return null;
    };
  }

  public static Class<?> toClass(Object o) {
    if (o instanceof JavaType) {
      return ((JavaType) o).getJavaClass();
    }
    return o.getClass();
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] newArray(Class<? extends T> type, int length) {
    return (T[]) Array.newInstance(type, length);
  }

  public static String getDebugName(Class<?> cls) {
    return Skript.logVeryHigh() ? cls.getName() : cls.getSimpleName();
  }

  public static void clearSectionNode(SectionNode node) {
    List<Node> subNodes = new ArrayList<>();
    node.forEach(subNodes::add);
    subNodes.forEach(Node::remove);
  }

  public static Class<?> getClass(Object o) {
    if (o instanceof ArrayWrapper) {
      return ((ArrayWrapper) o).getArray().getClass();
    }

    if (o == null) {
      return Object.class;
    }

    return o.getClass();
  }
}
