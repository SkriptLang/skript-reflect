package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Parameter;
import ch.njol.skript.log.*;
import ch.njol.skript.variables.Variables;
import org.bukkit.event.Event;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SkriptReflection {
  private static Field PATTERNS;
  private static Field PARAMETERS;
  private static Field HANDLERS;
  private static Field LOCAL_VARIABLES;
  private static Field VARIABLES_MAP_HASHMAP;
  private static Field VARIABLES_MAP_TREEMAP;
  private static Constructor VARIABLES_MAP;

  static {
    Field _FIELD;
    Constructor _CONSTRUCTOR;

    try {
      _FIELD = SyntaxElementInfo.class.getDeclaredField("patterns");
      _FIELD.setAccessible(true);
      PATTERNS = _FIELD;
    } catch (NoSuchFieldException e) {
      Skript.warning("Skript's pattern info field could not be resolved. " +
          "Custom syntax will not work.");
    }

    try {
      _FIELD = Function.class.getDeclaredField("parameters");
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

    try {
      _FIELD = Variables.class.getDeclaredField("localVariables");
      _FIELD.setAccessible(true);
      LOCAL_VARIABLES = _FIELD;
    } catch (NoSuchFieldException e) {
      Skript.warning("Skript's local variables field could not be resolved.");
    }

    try {
      Class<?> variablesMap = Class.forName("ch.njol.skript.variables.VariablesMap");

      try {
        _FIELD = variablesMap.getDeclaredField("hashMap");
        _FIELD.setAccessible(true);
        VARIABLES_MAP_HASHMAP = _FIELD;
      } catch (NoSuchFieldException e) {
        Skript.warning("Skript's hash map field could not be resolved.");
      }

      try {
        _FIELD = variablesMap.getDeclaredField("treeMap");
        _FIELD.setAccessible(true);
        VARIABLES_MAP_TREEMAP = _FIELD;
      } catch (NoSuchFieldException e) {
        Skript.warning("Skript's tree map field could not be resolved.");
      }

      try {
        _CONSTRUCTOR = variablesMap.getDeclaredConstructor();
        _CONSTRUCTOR.setAccessible(true);
        VARIABLES_MAP = _CONSTRUCTOR;
      } catch (NoSuchMethodException e) {
        Skript.warning("Skript's variables map constructor could not be resolved.");
      }
    } catch (ClassNotFoundException e) {
      Skript.warning("Skript's variables map class could not be resolved.");
    }
  }

  public static void setPatterns(SyntaxElementInfo<?> info, String[] patterns) {
    try {
      PATTERNS.set(info, patterns);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  public static Parameter<?>[] getParameters(Function function) {
    try {
      return ((Parameter<?>[]) PARAMETERS.get(function));
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    throw new IllegalStateException();
  }

  public static void printLog(RetainingLogHandler logger) {
    logger.stop();
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

  @SuppressWarnings("unchecked")
  public static boolean hasLocalVariables(Event e) {
    try {
      return ((Map<Event, Object>) LOCAL_VARIABLES.get(null)).containsKey(e);
    } catch (IllegalAccessException ex) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  public static void copyVariablesMap(Event from, Event to) {
    if (from == null) {
      return;
    }

    try {
      Map<Event, Object> localVariables = (Map<Event, Object>) LOCAL_VARIABLES.get(null);
      Object originalVariablesMap = localVariables.get(from);

      if (originalVariablesMap != null) {
        Object variablesMap = localVariables
            .computeIfAbsent(to, JavaUtil.propagateErrors(e -> VARIABLES_MAP.newInstance()));

        ((Map<String, Object>) VARIABLES_MAP_HASHMAP.get(variablesMap))
            .putAll((Map<String, Object>) VARIABLES_MAP_HASHMAP.get(originalVariablesMap));
        ((Map<String, Object>) VARIABLES_MAP_TREEMAP.get(variablesMap))
            .putAll((Map<String, Object>) VARIABLES_MAP_TREEMAP.get(originalVariablesMap));
      }
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
