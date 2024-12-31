package com.btk5h.skriptmirror;

import ch.njol.skript.registrations.Classes;
import com.btk5h.skriptmirror.util.JavaUtil;

import java.util.Objects;

public class ObjectWrapper {

	protected Object object;

	private ObjectWrapper(Object object) {
		this.object = object;
	}

	public static ObjectWrapper create(Object object) {
		if (object instanceof ObjectWrapper) {
			return (ObjectWrapper) object;
		}

		return new ObjectWrapper(object);
	}

	public static Object wrapIfNecessary(Object returnedValue, boolean forceWrap) {
		Class<?> returnedClass = returnedValue.getClass();
		if (returnedClass.isArray()) {
			returnedValue = create(JavaUtil.boxPrimitiveArray(returnedValue));
		} else if (forceWrap || Classes.getSuperClassInfo(returnedClass).getC() == Object.class) {
			returnedValue = create(returnedValue);
		}
		return returnedValue;
	}

	public static Object unwrapIfNecessary(Object o) {
		if (o instanceof ObjectWrapper) {
			return ((ObjectWrapper) o).get();
		}

		return o;
	}

	public Object get() {
		return object;
	}

	public boolean isArray() {
		if (object != null) {
			return object.getClass().isArray();
		}
		return false;
	}

	@Override
	public String toString() {
		if (isArray()) {
			return JavaUtil.arrayToString(object, Object::toString);
		}

		return object.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ObjectWrapper that = (ObjectWrapper) o;
		return Objects.equals(object, that.object);
	}

	@Override
	public int hashCode() {
		return Objects.hash(object);
	}

}
