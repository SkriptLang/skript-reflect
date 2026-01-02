package org.skriptlang.reflect.syntax.custom.shared;

public interface Continuable {

	default void markContinue() {
		setContinue(true);
	}

	void setContinue(boolean b);

}
