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

public class ExprParseMark extends SimpleExpression<Integer> implements EventRestrictedSyntax {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprParseMark.class, Integer.class)
			.supplier(ExprParseMark::new)
			.addPattern("[the] [parse[r]] mark")
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
	protected Integer @Nullable [] get(Event event) {
		if (!(event instanceof CustomSyntaxEvent syntaxEvent))
			return new Integer[0];
		return new Integer[] {syntaxEvent.parseResult().mark};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "the parse mark";
	}

}
