package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Parameter;
import ch.njol.skript.log.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SkriptReflection {
  private static Field PATTERNS;
  private static Field PARAMETERS;
  private static Field HANDLERS;

  static {
    Field _FIELD;
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

  public static Parameter<?>[] getParameters(Function function) {
    if (PARAMETERS != null) {
      try {
        return ((Parameter<?>[]) PARAMETERS.get(function));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    throw new IllegalStateException();
  }

  public static void printLog(RetainingLogHandler logger) {
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
}
