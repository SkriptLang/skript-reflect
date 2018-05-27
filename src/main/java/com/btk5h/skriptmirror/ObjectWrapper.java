package com.btk5h.skriptmirror;

import ch.njol.skript.registrations.Classes;
import com.btk5h.skriptmirror.util.JavaUtil;

import java.util.Arrays;

public class ObjectWrapper {
  protected Object object;

  private ObjectWrapper(Object object) {
    this.object = object;
  }

  public static ObjectWrapper create(Object object) {
    if (object instanceof ObjectWrapper) {
      return (ObjectWrapper) object;
    }

    if (object.getClass().isArray()) {
      return new OfArray((Object[]) object);
    }

    return new ObjectWrapper(object);
  }

  public static Object wrapIfNecessary(Object returnedValue) {
    Class<?> returnedClass = returnedValue.getClass();
    if (returnedClass.isArray()) {
      returnedValue = create(JavaUtil.boxPrimitiveArray(returnedValue));
    } else if (Classes.getSuperClassInfo(returnedClass).getC() == Object.class) {
      returnedValue = create(returnedValue);
    }
    return returnedValue;
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
