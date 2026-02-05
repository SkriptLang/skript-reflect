package com.btk5h.skriptmirror;

import ch.njol.skript.effects.EffReturn;
import com.btk5h.skriptmirror.skript.EffExpressionStatement;
import com.btk5h.skriptmirror.skript.custom.ExprMatchedPattern;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.skriptlang.reflect.syntax.expression.elements.StructCustomExpression;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.bukkit.registration.BukkitRegistryKeys;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Explicitly declares the relative parse orders of different statement types. Classes at the start of the list should
 * be parsed before classes at the end of the list.
 * <p>
 * This class should only be used to guarantee that skript-mirror's syntax is parsed before other addons. It cannot
 * guarantee that another addon's syntax will be parsed before skript-reflect.
 */
@SuppressWarnings("UnstableApiUsage")
public class ParseOrderWorkarounds {

	private static final Priority POSITION = Priority.before(SyntaxInfo.PATTERN_MATCHES_EVERYTHING);

	private static final String[] PARSE_ORDER = {
		EffExpressionStatement.class.getCanonicalName(),
		"com.w00tmast3r.skquery.elements.conditions.CondBoolean",
		"com.pie.tlatoani.Miscellaneous.CondBoolean",
		"us.tlatoani.tablisknu.core.base.CondBoolean",
		"com.pie.tlatoani.CustomEvent.EvtCustomEvent",
		EffReturn.class.getCanonicalName(),
		"ch.njol.skript.effects.EffContinue",
		"com.ankoki.skjade.elements.conditions.CondBoolean"
	};

	public static void reorderSyntax() {
		for (String c : PARSE_ORDER) {
			ensureLast(SyntaxRegistry.CONDITION, o -> o.type().getName().equals(c));
			ensureLast(SyntaxRegistry.EFFECT, o -> o.type().getName().equals(c));
			ensureLast(SyntaxRegistry.EXPRESSION, o -> o.type().getName().equals(c));
			ensureLast(BukkitSyntaxInfos.Event.KEY, o -> o.type().getName().equals(c));
			ensureLast(SyntaxRegistry.STRUCTURE, o -> o.type().getName().equals(c));
		}
	}

	private static <T> void ensureLast(SyntaxRegistry.Key<? extends SyntaxInfo<? extends T>> elementKey, Predicate<SyntaxInfo<? extends T>> checker) {
		SyntaxRegistry syntaxRegistry = SkriptMirror.getAddonInstance().syntaxRegistry();
		Optional<? extends SyntaxInfo<? extends T>> optionalE = syntaxRegistry.syntaxes(elementKey).stream()
			.filter(checker)
			.findFirst();

		optionalE.ifPresent(value -> {
			syntaxRegistry.unregister((SyntaxRegistry.Key) elementKey, value);
			var newInfo = value.toBuilder().priority(POSITION).build();
			syntaxRegistry.register((SyntaxRegistry.Key) elementKey, newInfo);
		});
	}

}
