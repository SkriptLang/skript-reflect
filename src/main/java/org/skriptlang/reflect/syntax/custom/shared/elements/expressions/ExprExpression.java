package org.skriptlang.reflect.syntax.custom.shared.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxStructure.ExpressionsData;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprExpression extends SimpleExpression<Object> {

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
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!(getParser().getCurrentStructure() instanceof CustomSyntaxStructure<?> structure)) {
			Skript.error("The 'expression' expression can only be used in a custom syntax structure");
			return false;
		}

		this.index = Integer.parseInt(parseResult.regexes.get(0).group()) ;
		if (this.index < 1) {
			Skript.error("The expression index must be at least 1.");
			return false;
		}

		ExpressionsData data = structure.expressionsData();
		if (this.index > data.expressions()) {
			Skript.error("Cannot get the " + StringUtils.fancyOrderNumber(index) + " as there are " + (data.expressions() == 0
				? "no expressions"
				: "only " + Utils.toEnglishPlural(data.expressions() + " expression", data.expressions() > 1)));
			return false;
		}
		this.all = parseResult.hasTag("all");
		this.plural = all || parseResult.hasTag("s");
		possibleReturnTypes = data.possibleReturnTypes(index - 1);
		if (!data.testPlurality(index - 1, plural)) {
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
