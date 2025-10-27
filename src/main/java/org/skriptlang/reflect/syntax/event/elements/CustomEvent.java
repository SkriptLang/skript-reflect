package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;

import java.util.Arrays;

/**
 * This is the Skript event for all custom events.
 * {@link #lastWhich} returns the first EventSyntaxInfo of the last custom event, which is used internally for determining
 * which event-values can be used in parse-time.
 */
public class CustomEvent extends SkriptEvent {

	public static EventSyntaxInfo lastWhich;

	private EventSyntaxInfo which;
	private Expression<?>[] exprs;
	private SkriptParser.ParseResult parseResult;
	private Object variablesMap;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		// prevent the user from using the placeholder pattern we register in order to satisfy the registration requirements
		if (matchedPattern == 0) {
			return false;
		}

		which = StructCustomEvent.lookup(SkriptUtil.getCurrentScript(), matchedPattern);

		if (which == null) {
			return false;
		}

		this.exprs = Arrays.stream(args)
			.map(SkriptUtil::defendExpression)
			.toArray(Expression[]::new);
		this.parseResult = parseResult;

		if (!SkriptUtil.canInitSafely(this.exprs)) {
			return false;
		}

		Boolean bool = StructCustomEvent.parseSectionLoaded.get(which);
		if (bool != null && !bool) {
			Skript.error("You can't use custom events with parse sections before they're loaded.");
			return false;
		}

		Trigger parseHandler = StructCustomEvent.parserHandlers.get(which);

		if (parseHandler == null) {
			setLastWhich(which);

			return true;
		}

		SyntaxParseEvent event =
			new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, getParser().getCurrentEvents());

		setLastWhich(which);

		TriggerItem.walk(parseHandler, event);
		variablesMap = SkriptReflection.removeLocals(event);

		setLastWhich(which);

		return event.isMarkedContinue();
	}

	@Override
	public boolean load() {
		CustomEvent.setLastWhich(which);
		boolean parsed = super.load();
		CustomEvent.setLastWhich(null);
		return parsed;
	}

	@Override
	public boolean check(Event e) {
		BukkitCustomEvent bukkitCustomEvent = (BukkitCustomEvent) e;
		if (!bukkitCustomEvent.getName().equalsIgnoreCase(StructCustomEvent.nameValues.get(which)))
			return false;

		EventTriggerEvent eventTriggerEvent = new EventTriggerEvent(e, exprs, which.getMatchedPattern(), parseResult, which.getPattern());
		SkriptReflection.putLocals(SkriptReflection.copyLocals(variablesMap), eventTriggerEvent);

		Trigger trigger = StructCustomEvent.eventHandlers.get(which);
		if (trigger != null) {
			trigger.execute(eventTriggerEvent);
			return eventTriggerEvent.isMarkedContinue();
		}

		return true;
	}

	public static void setLastWhich(EventSyntaxInfo which) {
		lastWhich = which;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return which.getPattern();
	}

}
