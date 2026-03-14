package org.skriptlang.reflect.syntax.custom;

import com.btk5h.skriptmirror.SkriptMirror;
import org.skriptlang.reflect.registration.OriginApplyingSyntaxRegistry;
import org.skriptlang.reflect.syntax.custom.computedoption.elements.StructComputedOption;
import org.skriptlang.reflect.syntax.custom.condition.elements.EffNegateCondition;
import org.skriptlang.reflect.syntax.custom.condition.elements.StructCustomCondition;
import org.skriptlang.reflect.syntax.custom.effect.elements.EffDelayEffect;
import org.skriptlang.reflect.syntax.custom.effect.elements.StructCustomEffect;
import org.skriptlang.reflect.syntax.custom.event.elements.*;
import org.skriptlang.reflect.syntax.custom.expression.elements.ExprChangeValue;
import org.skriptlang.reflect.syntax.custom.expression.elements.StructCustomExpression;
import org.skriptlang.reflect.syntax.custom.shared.elements.effects.EffContinue;
import org.skriptlang.reflect.syntax.custom.shared.elements.expressions.*;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CustomSyntaxModule implements AddonModule {

	public static ModuleOrigin ORIGIN = AddonModule.origin(SkriptMirror.getAddonInstance(), new CustomSyntaxModule());

	@Override
	public void load(SkriptAddon addon) {
		SyntaxRegistry registry = new OriginApplyingSyntaxRegistry(addon.syntaxRegistry(), ORIGIN);
		register(
			registry,

			// ==Computed Option==
			StructComputedOption::register,

			// ==Custom Condition==
			EffNegateCondition::register,
			StructCustomCondition::register,

			// ==Custom Effect==
			EffDelayEffect::register,
			StructCustomEffect::register,

			// ==Custom Event==
			CondEventCancelled::register,
			EffCallEvent::register,
			ExprCustomEvent::register,
			ExprEventData::register,
			StructCustomEvent::register,

			// ==Custom Expression==
			StructCustomExpression::register,
			ExprChangeValue::register,

			// ==Shared==
			EffContinue::register,
			ExprEventClasses::register,
			ExprExpression::register,
			ExprMatchedPattern::register,
			ExprParseMark::register,
			ExprParseRegexes::register,
			ExprParseTags::register,
			ExprRawExpression::register,
			ExprSelf::register
		);
	}

	@Override
	public String name() {
		return "custom syntax";
	}

	@SafeVarargs
	private static void register(SyntaxRegistry registry, Consumer<SyntaxRegistry>... consumers) {
		for (Consumer<SyntaxRegistry> consumer : consumers)
			consumer.accept(registry);
	}
}
