package org.skriptlang.reflect.syntax.custom.expression;

import ch.njol.skript.lang.Trigger;

public record ChangerTrigger(Trigger trigger, Class<?>[] acceptedClasses) {
}
