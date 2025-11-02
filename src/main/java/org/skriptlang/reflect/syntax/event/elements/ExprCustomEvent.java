package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;

public class ExprCustomEvent extends SimpleExpression<Event> {

	static {
		Skript.registerExpression(ExprCustomEvent.class, Event.class, ExpressionType.PATTERN_MATCHES_EVERYTHING,
			"[a] [new] custom event %string% [(with|using)] data %-objects%",
			"[a] [new] custom event %string% [(with|using) [[event-]values] %-objects%] [[and] [(with|using)] data %-objects%]");
	}

	private Expression<String> customEventName;
	private Variable<?> eventValueVarList;
	private Variable<?> dataVarList;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (!SkriptUtil.canInitSafely(exprs))
			return false;

		this.customEventName = SkriptUtil.defendExpression(exprs[0]);

		if (matchedPattern == 1) {
			if (exprs[1] != null) {
				Expression<?> var = SkriptUtil.defendExpression(exprs[1]);

				if (var instanceof Variable && ((Variable<?>) var).isList()) {
					this.eventValueVarList = (Variable<?>) var;
				} else {
					Skript.error(var.toString(null, false) + " is not a list variable.");
					return false;
				}
			}
		}

		Expression<?> expr = exprs[matchedPattern == 0 ? 1 : 2];
		if (expr != null) {
			Expression<?> var = SkriptUtil.defendExpression(expr);

			if (var instanceof Variable && ((Variable<?>) var).isList()) {
				this.dataVarList = (Variable<?>) var;
			} else {
				Skript.error(var.toString(null, false) + " is not a list variable.");
				return false;
			}
		}

		return SkriptUtil.canInitSafely(customEventName, eventValueVarList, dataVarList);
	}

	@Nullable
	@Override
	protected Event[] get(Event e) {
		String name = this.customEventName.getSingle(e);
		if (name == null)
			return null;
		BukkitCustomEvent bukkitCustomEvent = new BukkitCustomEvent(name);

		if (eventValueVarList != null)
			eventValueVarList.variablesIterator(e).forEachRemaining(pair -> {
				if (pair.getKey() == null)
					return;
				ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(pair.getKey());
				bukkitCustomEvent.setEventValue(classInfo, pair.getValue());
			});

		if (dataVarList != null)
			dataVarList.variablesIterator(e).forEachRemaining(pair -> {
				bukkitCustomEvent.setData(pair.getKey(), pair.getValue());
			});

		return new Event[] {bukkitCustomEvent};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Event> getReturnType() {
		return Event.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "new custom event " + customEventName.toString(e, debug);
	}

}
