package org.skriptlang.reflect.syntax.custom.shared;

import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.Trigger;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

public interface CustomSyntax extends SyntaxElement {

	Priority PRIORITY = SyntaxInfo.COMBINED, LOCAL_PRIORITY = Priority.before(PRIORITY);

	CustomSyntaxInfo<? extends CustomSyntax> info();

}
