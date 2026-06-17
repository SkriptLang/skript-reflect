package org.skriptlang.reflect.syntax.custom.shared.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprSelf extends SimpleExpression<SyntaxElement> {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprSelf.class, SyntaxElement.class)
			.supplier(ExprSelf::new)
			.addPattern("self")
			.build());
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentEvent(CustomSyntaxEvent.class)) {
			Skript.error("The self expression can only be used in a custom syntax structure");
			return false;
		}
		return true;
	}

	@Override
	protected SyntaxElement @Nullable [] get(Event event) {
		return event instanceof CustomSyntaxEvent syntaxEvent ? new SyntaxElement[] {syntaxEvent.self()} : new SyntaxElement[0];
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends SyntaxElement> getReturnType() {
		return SyntaxElement.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "self";
	}

}
