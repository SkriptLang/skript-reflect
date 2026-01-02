package org.skriptlang.reflect.syntax.custom.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.custom.expression.elements.StructCustomExpression.ChangerData;
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
	private Class<?>[] types;

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return CollectionUtils.array(ExpressionChangeEvent.class);
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		this.plural = parseResult.hasTag("s");
		ChangerData data = getParser().getData(ChangerData.class);
		this.types = data.acceptedClasses();
		if (!data.testPlurality(plural)) {
			Skript.error(plural
				? "The changed value may only be a single value"
				: "The changed value cannot be a single value");
			return false;
		}
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
		return Utils.getSuperType(types);
	}

	@Override
	public Class<?>[] possibleReturnTypes() {
		return types;
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
