package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.sections.SecLoop;
import ch.njol.skript.sections.SecWhile;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.skript.custom.Continuable;
import com.btk5h.skriptmirror.skript.reflect.sections.SectionEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.expression.ConstantGetEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionSyntaxInfo;
import org.skriptlang.skript.lang.structure.Structure;

public class EffReturn extends Effect {

	static {
		Skript.registerEffect(EffReturn.class, "return [%-objects%]");
	}

	private Expression<?> objects;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		Expression<?> expr = SkriptUtil.defendExpression(exprs[0]);
		if (!SkriptUtil.canInitSafely(expr)) {
			Skript.error("Can't understand this expression: " + expr);
			return false;
		}

		boolean isContinuable = CollectionUtils.containsAnySuperclass(new Class[]{Continuable.class}, getParser().getCurrentEvents());

		if (!getParser().isCurrentEvent(ExpressionGetEvent.class, ConstantGetEvent.class, SectionEvent.class)
				&& !isContinuable) {
			Skript.error("The return effect can only be used in functions, custom expressions, sections, custom syntax parse sections and custom conditions");
			return false;
		}

		if (isContinuable) {
			expr = expr.getConvertedExpression(Boolean.class);
			if (expr == null || !expr.isSingle()) {
				Skript.error(exprs[0] + " is not a single boolean value");
				return false;
			}
		}

		Structure structure = getParser().getCurrentStructure();
		if (expr != null && structure instanceof StructCustomExpression) {
			StructCustomExpression customExpressionSection = (StructCustomExpression) structure;
			ExpressionSyntaxInfo which = customExpressionSection.getFirstWhich();
			Class<?> returnType = StructCustomExpression.returnTypes.get(which);
			if (returnType != null) {
				Expression<?> newExpr = expr.getConvertedExpression(returnType);
				if (newExpr == null) {
					Skript.error(expr + " is not " + Classes.getSuperClassInfo(returnType).getName().withIndefiniteArticle());
					return false;
				}
				expr = newExpr;
			}
		}

		if (!isDelayed.isFalse()) {
			Skript.error("Return may not be used if the code before it contains any delays", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}

		objects = expr;

		return true;
	}

	@Override
	protected TriggerItem walk(Event e) {
		if (e instanceof SectionEvent) {
			((SectionEvent) e).setOutput(objects == null ? new Object[0] : objects.getArray(e));
		} else if (e instanceof ExpressionGetEvent) {
			((ExpressionGetEvent) e).setOutput(objects == null ? new Object[0] : objects.getArray(e));
		} else {
			// objects is always a single boolean expression, see init
			// Doesn't require casting, and deals with null
			boolean b = Boolean.TRUE.equals(objects.getSingle(e));
			((Continuable) e).setContinue(b);
		}

		TriggerSection parent = getParent();
		while (parent != null) {
			if (parent instanceof SecLoop) {
				((SecLoop) parent).exit(e);
			} else if (parent instanceof SecWhile) {
				((SecWhile) parent).exit(e);
			}
			parent = parent.getParent();
		}

		return null;
	}

	@Override
	protected void execute(Event e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString(Event e, boolean debug) {
		if (objects == null) {
			return "return";
		}
		return "return " + objects.toString(e, debug);
	}

}
