package com.btk5h.skriptmirror;

import java.util.Objects;

public final class JavaType {

	private final Class<?> javaClass;

	public JavaType(Class<?> javaClass) {
		this.javaClass = javaClass;
	}

	public Class<?> getJavaClass() {
		return javaClass;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JavaType javaType1 = (JavaType) o;
		return Objects.equals(javaClass, javaType1.javaClass);
	}

	@Override
	public int hashCode() {
		return Objects.hash(javaClass);
	}

}
