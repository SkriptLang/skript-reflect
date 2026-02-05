package org.skriptlang.reflect.syntax.custom.shared.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxEvent;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.List;
import java.util.regex.MatchResult;

public class ExprParseRegexes extends SimpleExpression<String> implements EventRestrictedSyntax {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprParseRegexes.class, String.class)
			.origin(Origin.of(SkriptMirror.getAddonInstance()))
			.supplier(ExprParseRegexes::new)
			.addPattern("[the] [parse[r]] (regex|regular expression)(-| )<\\d+>")
			.priority(SyntaxInfo.SIMPLE)
			.build());
	}

	private int index;

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
		return true;
	}

	@Override
	protected String @Nullable [] get(Event event) {
		if (!(event instanceof CustomSyntaxEvent syntaxEvent))
			return new String[0];
		List<MatchResult> regexes = syntaxEvent.parseResult().regexes;
		if (index >= regexes.size())
			return new String[0];
		MatchResult match = regexes.get(index);
		int groupCount = match.groupCount();
		String[] groups = new String[groupCount];
		for (int i = 0; i < groups.length; i++)
			groups[i] = match.group(i + 1);
		return groups;
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
		return "the parse regex-" + (index + 1);
	}

}
