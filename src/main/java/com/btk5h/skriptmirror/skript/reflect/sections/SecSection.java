package com.btk5h.skriptmirror.skript.reflect.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.Variable;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SecSection extends ch.njol.skript.lang.Section {

	public static boolean sectionsUsed = false;

	static {
		Skript.registerSection(SecSection.class,
			"create [new] section [with [arguments variables] %-objects%] (and store it|stored) in %objects%");
	}

	private Trigger trigger;
	private ArrayList<Variable<?>> variableArguments;
	private Expression<Object> variableStore;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
		sectionsUsed = true;

		Expression<?> varList = SkriptUtil.defendExpression(exprs[0]);
		variableStore = SkriptUtil.defendExpression(exprs[1]);

		if (!SkriptUtil.canInitSafely(varList, variableStore))
			return false;

		variableArguments = new ArrayList<>();
		if (varList != null) {
			if (varList instanceof ExpressionList) {
				ExpressionList<?> expressionList = (ExpressionList<?>) varList;

				for (Expression<?> expr : expressionList.getExpressions()) {
					if (!(expr instanceof Variable)) {
						Skript.error("The arguments can only contain variables");
						return false;
					}

					variableArguments.add((Variable<?>) expr);
				}
			} else if (varList instanceof Variable) {
				variableArguments.add((Variable<?>) varList);
			} else {
				Skript.error("The arguments can only contain variables");
				return false;
			}
		}

		if (!(variableStore instanceof Variable)) {
			Skript.error("The created section can only be stored in a variable");
			return false;
		}

		trigger = loadCode(sectionNode, "section", SectionEvent.class);

		return SkriptUtil.canInitSafely(variableStore);
	}

	@Override
	protected @Nullable TriggerItem walk(Event e) {
		Section section = new Section(trigger, e, variableArguments);
		variableStore.change(e, new Section[]{section}, Changer.ChangeMode.SET);
		return super.walk(e, false);
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		StringBuilder stringBuilder = new StringBuilder("create section");
		if (!variableArguments.isEmpty()) {
			stringBuilder.append(" with argument variables ");
			for (int i = 0; i < variableArguments.size(); i++) {
				Variable<?> variable = variableArguments.get(i);
				stringBuilder.append(variable.toString(e, debug));

				if (i == variableArguments.size() - 2) {
					stringBuilder.append(" and ");
				} else if (i != variableArguments.size() - 1) {
					stringBuilder.append(", ");
				}
			}
		}
		if (variableStore != null) {
			stringBuilder.append(" stored in ").append(variableStore.toString(e, debug));
		}
		return stringBuilder.toString();
	}

}
