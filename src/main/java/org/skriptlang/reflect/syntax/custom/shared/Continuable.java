package org.skriptlang.reflect.syntax.custom.shared;

public interface Continuable {

	boolean isMarkedContinue();

	default void markContinue() {
		setContinue(true);
	}

	void setContinue(boolean b);

}
