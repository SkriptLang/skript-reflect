package org.skriptlang.reflect.syntax.custom.expression.elements;

import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.expression.ExpressionChangeEvent;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxOrigin;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprChangeValue extends SimpleExpression<Object> implements EventRestrictedSyntax {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprChangeValue.class, Object.class)
			.origin(SyntaxOrigin.of(SkriptMirror.getAddonInstance()))
			.supplier(ExprChangeValue::new)
			.addPattern("[the] change value[:s]")
			.priority(SyntaxInfo.SIMPLE)
			.build());
	}

	private boolean plural;

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return CollectionUtils.array(ExpressionChangeEvent.class);
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		this.plural = parseResult.hasTag("s");
		return true;
	}

	@Override
	protected Object @Nullable [] get(Event event) {
		if (!(event instanceof ExpressionChangeEvent changeEvent))
			return new Object[0];
		return changeEvent.delta();
	}

	@Override
	public Class<?> getReturnType() {
		return Object.class;
	}

	@Override
	public boolean isSingle() {
		return !plural;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "change value" + (plural ? "s" : "");
	}

}
