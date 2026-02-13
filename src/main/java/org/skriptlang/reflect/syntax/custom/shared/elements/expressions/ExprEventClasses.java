package org.skriptlang.reflect.syntax.custom.shared.elements.expressions;

import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.JavaType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.custom.shared.SyntaxParseEvent;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Arrays;

public class ExprEventClasses extends SimpleExpression<JavaType> implements EventRestrictedSyntax {

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(ExprEventClasses.class, JavaType.class)
			.supplier(ExprEventClasses::new)
			.addPattern("[the] event-classes")
			.priority(SyntaxInfo.SIMPLE)
			.build());
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return CollectionUtils.array(SyntaxParseEvent.class);
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		return true;
	}

	@Override
	protected JavaType @Nullable [] get(Event event) {
		if (!(event instanceof SyntaxParseEvent parseEvent))
			return new JavaType[0];
		return Arrays.stream(parseEvent.eventClasses())
			.map(JavaType::new)
			.toArray(JavaType[]::new);
	}

	@Override
	public Class<? extends JavaType> getReturnType() {
		return JavaType.class;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "the event-classes";
	}

}
