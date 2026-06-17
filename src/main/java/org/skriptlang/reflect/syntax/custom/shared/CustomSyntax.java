package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.lang.SyntaxElement;

public interface CustomSyntax extends SyntaxElement {

	CustomSyntaxInfo<? extends CustomSyntax> info();

}
