package org.skriptlang.reflect.syntax.custom.shared.elements.expressions;

import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprParseTags extends SimpleExpression<String> implements EventRestrictedSyntax {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprParseTags.class, String.class)
			.supplier(ExprParseTags::new)
			.addPattern("[the] [parse[r]] tags")
			.priority(SyntaxInfo.SIMPLE)
			.build());
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return CollectionUtils.array(CustomSyntaxEvent.class);
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		return true;
	}

	@Override
	protected String @Nullable [] get(Event event) {
		if (!(event instanceof CustomSyntaxEvent syntaxEvent))
			return new String[0];
		return syntaxEvent.parseResult().tags.toArray(new String[0]);
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "the parse tags";
	}

}
