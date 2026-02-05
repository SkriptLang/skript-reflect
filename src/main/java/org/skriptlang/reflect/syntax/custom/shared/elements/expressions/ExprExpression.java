package org.skriptlang.reflect.syntax.custom.shared.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxStructure.ExpressionsData;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprExpression extends SimpleExpression<Object> implements EventRestrictedSyntax {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprExpression.class, Object.class)
			.origin(Origin.of(SkriptMirror.getAddonInstance()))
			.supplier(ExprExpression::new)
			.addPattern("[the|:all] expr[ession][:s](-| )<\\d+>")
			.priority(SyntaxInfo.SIMPLE)
			.build());
	}

	private int index;
	private boolean all, plural;
	private Class<?>[] possibleReturnTypes;

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return CollectionUtils.array(CustomSyntaxEvent.class);
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		this.index = Integer.parseInt(parseResult.regexes.get(0).group()) - 1;
		if (this.index < 0) {
			Skript.error("The expression index must be at least 1.");
			return false;
		}
		this.all = parseResult.hasTag("all");
		this.plural = all || parseResult.hasTag("s");
		ExpressionsData data = getParser().getData(ExpressionsData.class);
		possibleReturnTypes = data.possibleReturnTypes(index);
		if (!data.testPlurality(index, plural)) {
			String expression = "The " + StringUtils.fancyOrderNumber(index) + " expression";
			Skript.error(plural
				? expression + " can only be a single value"
				: expression + " may return more than one value");
			return false;
		}
		return true;
	}

	@Override
	protected Object @Nullable [] get(Event event) {
		if (!(event instanceof CustomSyntaxEvent syntaxEvent))
			return new Object[0];
		Expression<?>[] expressions = syntaxEvent.expressions();
		if (index >= expressions.length)
			return new Object[0];
		return all ? expressions[index].getAll(event) : expressions[index].getArray(event);
	}

	@Override
	public Class<?> getReturnType() {
		return Utils.getSuperType(possibleReturnTypes);
	}

	@Override
	public Class<?>[] possibleReturnTypes() {
		return possibleReturnTypes;
	}

	@Override
	public boolean isSingle() {
		return !plural;
	}

	public int index() {
		return index;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return (all ? "all" : "") + " expression" + (plural ? "s" : "") + "-" + (index + 1);
	}

}
