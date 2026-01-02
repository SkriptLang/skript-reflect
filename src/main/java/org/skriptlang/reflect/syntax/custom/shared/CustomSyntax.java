package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.Trigger;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

public interface CustomSyntax<I extends SyntaxInfo<?>> extends SyntaxElement {

	Priority PRIORITY = SyntaxInfo.COMBINED, LOCAL_PRIORITY = Priority.before(PRIORITY);

	SyntaxRegistry.Key<I> key();

	I info();

	default boolean register(SyntaxRegistry registry) {
		registry.register(key(), info());
		return true;
	}

	default void unregister(SyntaxRegistry registry) {
		registry.unregister(key(), info());
	}

	CustomSyntax<I> copy();

	Trigger parseTrigger();

	void parseTrigger(Trigger trigger);

}
