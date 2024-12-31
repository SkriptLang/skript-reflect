package com.btk5h.skriptmirror.skript.custom;

public interface Continuable {

	default void markContinue() {
		setContinue(true);
	}

	void setContinue(boolean b);

}
