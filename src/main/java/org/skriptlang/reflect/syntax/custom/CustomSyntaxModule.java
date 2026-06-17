package org.skriptlang.reflect.syntax.custom;

import com.btk5h.skriptmirror.JavaType;
import org.skriptlang.reflect.syntax.custom.computedoption.elements.StructComputedOption;
import org.skriptlang.reflect.syntax.custom.condition.elements.EffNegateCondition;
import org.skriptlang.reflect.syntax.custom.condition.elements.StructCustomCondition;
import org.skriptlang.reflect.syntax.custom.effect.elements.EffDelayEffect;
import org.skriptlang.reflect.syntax.custom.effect.elements.StructCustomEffect;
import org.skriptlang.reflect.syntax.custom.event.CustomEventManager;
import org.skriptlang.reflect.syntax.custom.event.elements.*;
import org.skriptlang.reflect.syntax.custom.expression.elements.ExprChangeValue;
import org.skriptlang.reflect.syntax.custom.expression.elements.StructCustomExpression;
import org.skriptlang.reflect.syntax.custom.shared.SyntaxParseEvent;
import org.skriptlang.reflect.syntax.custom.shared.elements.effects.EffContinue;
import org.skriptlang.reflect.syntax.custom.shared.elements.expressions.*;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;

import java.util.Arrays;

public class CustomSyntaxModule implements AddonModule {

	@Override
	public void load(SkriptAddon addon) {
		CustomEventManager.init();
		register(addon,
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
			ExprExpression::register,
			ExprMatchedPattern::register,
			ExprParseMark::register,
			ExprParseRegexes::register,
			ExprParseTags::register,
			ExprRawExpression::register,
			ExprSelf::register
		);

		addon.registry(EventValueRegistry.class).register(EventValue.builder(SyntaxParseEvent.class, JavaType[].class)
			.getter(event -> Arrays.stream(event.eventClasses())
				.map(JavaType::new)
				.toArray(JavaType[]::new))
			.patterns("classes")
			.build());
	}

	@Override
	public String name() {
		return "custom syntax";
	}

}
