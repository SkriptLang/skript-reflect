package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;

import java.lang.reflect.Array;

@SuppressWarnings("unused")
public class ExprCustomEventValue<T> extends EventValueExpression<T> {

	static {
		//noinspection unchecked
		Skript.registerExpression(ExprCustomEventValue.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, "[the] [event-]<.+>");
	}

	private ClassInfo<? super T> classInfo;
	private Changer<? super T> changer;

	@SuppressWarnings("unchecked")
	public ExprCustomEventValue() {
		super((Class<? extends T>) Object.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final SkriptParser.ParseResult parseResult) {
		if (!getParser().isCurrentEvent(BukkitCustomEvent.class, EventTriggerEvent.class))
			return false;
		EventSyntaxInfo which = CustomEvent.lastWhich;
		if (which == null)
			return false;

		String stringClass = parseResult.regexes.get(0).group();
		classInfo = (ClassInfo<? super T>) Classes.getClassInfoFromUserInput(stringClass);
		if (classInfo == null) {
			return false;
		}

		if (!CustomEventUtils.hasEventValue(which, classInfo)) {
			Skript.error("There is no " + CustomEventUtils.getName(classInfo) + " in the custom event " +
				CustomEventUtils.getName(which));
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T[] get(Event event) {
		if (!(event instanceof BukkitCustomEvent || event instanceof EventTriggerEvent))
			return null;
		BukkitCustomEvent bukkitCustomEvent;
		if (event instanceof BukkitCustomEvent) {
			bukkitCustomEvent = (BukkitCustomEvent) event;
		} else {
			bukkitCustomEvent = (BukkitCustomEvent) ((EventTriggerEvent) event).getDirectEvent();
		}

		T[] tArray = (T[]) Array.newInstance(classInfo.getC(), 1);
		tArray[0] = (T) bukkitCustomEvent.getEventValue(classInfo);
		return tArray;
	}

	@Override
	public Class<?>[] acceptChange(Changer.ChangeMode mode) {
		changer = classInfo.getChanger();
		return changer == null ? null : changer.acceptChange(mode);
	}

	@Override
	public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
		if (changer == null)
			throw new UnsupportedOperationException();
		Changer.ChangerUtils.change(changer, getArray(e), delta, mode);
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return "event-" + classInfo.getCodeName();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<T> getReturnType() {
		return (Class<T>) classInfo.getC();
	}

}
