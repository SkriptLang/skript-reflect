package com.btk5h.skriptmirror;

import ch.njol.skript.log.HandlerList;
import ch.njol.skript.log.LogHandler;
import ch.njol.util.coll.iterator.CombinedIterator;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class InsertingHandlerList extends HandlerList {
  private LogHandler insertedHandler;

  public InsertingHandlerList(LogHandler insertedHandler) {
    this.insertedHandler = insertedHandler;
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public Iterator<LogHandler> iterator() {
    List<Iterable<LogHandler>> iterators = Arrays.asList(
        Collections.singleton(insertedHandler),
        super::iterator
    );
    return new CombinedIterator<>(iterators.iterator());
  }
}
