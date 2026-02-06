package org.skriptlang.reflect.syntax.custom;

import org.skriptlang.reflect.syntax.custom.computedoption.elements.StructComputedOption;
import org.skriptlang.reflect.syntax.custom.condition.elements.EffNegateCondition;
import org.skriptlang.reflect.syntax.custom.condition.elements.StructCustomCondition;
import org.skriptlang.reflect.syntax.custom.effect.elements.EffDelayEffect;
import org.skriptlang.reflect.syntax.custom.effect.elements.StructCustomEffect;
import org.skriptlang.reflect.syntax.custom.event.elements.*;
import org.skriptlang.reflect.syntax.custom.expression.elements.ExprChangeValue;
import org.skriptlang.reflect.syntax.custom.expression.elements.StructCustomExpression;
import org.skriptlang.reflect.syntax.custom.shared.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.custom.shared.elements.effects.EffContinue;
import org.skriptlang.reflect.syntax.custom.shared.elements.expressions.*;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;

public class CustomSyntaxModule implements AddonModule {

	@Override
	public void load(SkriptAddon addon) {
		CustomSyntaxStructure.register();

		// ==Computed Option==
		StructComputedOption.register(addon.syntaxRegistry());

		// ==Custom Condition==
		EffNegateCondition.register(addon.syntaxRegistry());
		StructCustomCondition.register(addon.syntaxRegistry());

		// ==Custom Effect==
		EffDelayEffect.register(addon.syntaxRegistry());
		StructCustomEffect.register(addon.syntaxRegistry());

		// ==Custom Event==
		CondEventCancelled.register(addon.syntaxRegistry());
		EffCallEvent.register(addon.syntaxRegistry());
		ExprCustomEvent.register(addon.syntaxRegistry());
		ExprEventData.register(addon.syntaxRegistry());
		StructCustomEvent.register(addon.syntaxRegistry());

		// ==Custom Expression==
		StructCustomExpression.register(addon.syntaxRegistry());
		ExprChangeValue.register(addon.syntaxRegistry());

		// ==Shared==
		EffContinue.register(addon.syntaxRegistry());
		ExprEventClasses.register(addon.syntaxRegistry());
		ExprExpression.register(addon.syntaxRegistry());
		ExprMatchedPattern.register(addon.syntaxRegistry());
		ExprParseMark.register(addon.syntaxRegistry());
		ExprParseRegexes.register(addon.syntaxRegistry());
		ExprParseTags.register(addon.syntaxRegistry());
		ExprRawExpression.register(addon.syntaxRegistry());
		ExprSelf.register(addon.syntaxRegistry());
	}

	@Override
	public String name() {
		return "custom syntax";
	}

}
