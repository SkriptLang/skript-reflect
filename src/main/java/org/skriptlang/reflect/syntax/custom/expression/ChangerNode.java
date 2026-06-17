package org.skriptlang.reflect.syntax.custom.expression;

import ch.njol.skript.config.SectionNode;

public record ChangerNode(String name, SectionNode node, Class<?>[] acceptedClasses) {
}
