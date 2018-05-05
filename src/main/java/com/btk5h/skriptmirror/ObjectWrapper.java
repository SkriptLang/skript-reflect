package com.btk5h.skriptmirror;

import java.util.Arrays;

public class ObjectWrapper {
  protected Object object;

  private ObjectWrapper(Object object) {
    this.object = object;
  }

  public static ObjectWrapper create(Object object) {
    if (object.getClass().isArray()) {
      return new OfArray((Object[]) object);
    }

    return new ObjectWrapper(object);
  }

  public Object get() {
    return object;
  }

  @Override
  public String toString() {
    return object.toString();
  }

  public static class OfArray extends ObjectWrapper {
    private OfArray(Object[] object) {
      super(object);
    }

    @Override
    public Object[] get() {
      return (Object[]) object;
    }

    @Override
    public String toString() {
      return Arrays.deepToString(get());
    }
  }
}
