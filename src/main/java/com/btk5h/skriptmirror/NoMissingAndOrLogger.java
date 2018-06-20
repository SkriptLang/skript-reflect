package com.btk5h.skriptmirror;

import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import com.btk5h.skriptmirror.util.SkriptReflection;

public class NoMissingAndOrLogger extends LogHandler {
  @Override
  public LogResult log(LogEntry entry) {
    if (SkriptReflection.MISSING_AND_OR != null && entry.message.contains(SkriptReflection.MISSING_AND_OR)) {
      return LogResult.DO_NOT_LOG;
    }
    return LogResult.LOG;
  }
}
