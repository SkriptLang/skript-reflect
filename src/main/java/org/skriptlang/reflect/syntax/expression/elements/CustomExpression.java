package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionSyntaxInfo;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class CustomExpression<T> implements Expression<T> {

	private ExpressionSyntaxInfo which;
	private Expression<?>[] exprs;
	private SkriptParser.ParseResult parseResult;
	private Object variablesMap;

	private final CustomExpression<?> source;
	private Class<? extends T>[] types;
	private Class<T> superType;

	@SuppressWarnings({"unchecked", "unused"})
	public CustomExpression() {
		this(null, (Class<? extends T>) Object.class);
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	private CustomExpression(CustomExpression<?> source, Class<? extends T>... types) {
		this.source = source;

		if (source != null) {
			this.which = source.which;
			this.exprs = source.exprs;
			this.parseResult = source.parseResult;
			this.variablesMap = source.variablesMap;
		}

		this.types = types;
		this.superType = (Class<T>) Utils.getSuperType(types);
	}

	@Override
	public T getSingle(Event e) {
		T[] all = getAll(e);
		return all.length == 0 ? null : all[0];
	}

	@Override
	public T[] getArray(Event e) {
		return getAll(e);
	}

	@Override
	public T[] getAll(Event e) {
		Trigger getter = StructCustomExpression.expressionHandlers.get(which);

		if (getter == null) {
			Skript.error(
				String.format("The custom expression '%s' no longer has a get handler.",
					which.getPattern())
			);
			return JavaUtil.newArray(superType, 0);
		}

		if (which.isProperty()) {
			return getByProperty(e, getter);
		}

		return getByStandard(e, getter);
	}

	private T[] getByStandard(Event e, Trigger getter) {
		ExpressionGetEvent expressionEvent = new ExpressionGetEvent(e, exprs, which.getMatchedPattern(), parseResult);
		SkriptReflection.putLocals(variablesMap, expressionEvent);
		getter.execute(expressionEvent);
		if (expressionEvent.getOutput() == null) {
			Skript.error(
				String.format("The get handler for '%s' did not return.", which.getPattern())
			);
			return JavaUtil.newArray(superType, 0);
		}

		return Converters.convert(expressionEvent.getOutput(), types, superType);
	}

	private T[] getByProperty(Event e, Trigger getter) {
		List<T> output = new ArrayList<>();
		for (Object o : exprs[0].getArray(e)) {
			Expression<?>[] localExprs = Arrays.copyOf(exprs, exprs.length);
			localExprs[0] = new SimpleLiteral<>(o, false);

			ExpressionGetEvent expressionEvent =
				new ExpressionGetEvent(e, localExprs, which.getMatchedPattern(), parseResult);
			SkriptReflection.putLocals(variablesMap, expressionEvent);
			getter.execute(expressionEvent);

			Object[] exprOutput = expressionEvent.getOutput();
			if (exprOutput == null) {
				Skript.error(
					String.format("The get handler for '%s' did not return for the value %s",
						which.getPattern(), Classes.toString(o))
				);
				return JavaUtil.newArray(superType, 0);
			}

			if (!which.isAlwaysPlural() && exprOutput.length > 1) {
				Skript.error(
					String.format("The get handler for '%s' returned more than one value.", which.getPattern())
				);
				return JavaUtil.newArray(superType, 0);
			}

			T[] converted = Converters.convert(exprOutput, superType);
			output.addAll(Arrays.asList(converted));
		}

		return output.toArray(JavaUtil.newArray(superType, 0));
	}

	@Override
	public boolean isSingle() {
		return !which.isAlwaysPlural() &&
			Arrays.stream(which.getInheritedSingles())
				.mapToObj(i -> exprs[i])
				.filter(Objects::nonNull)
				.allMatch(Expression::isSingle);
	}

	@Override
	public boolean check(Event e, Predicate<? super T> c, boolean negated) {
		return SimpleExpression.check(getAll(e), c, negated, getAnd());
	}

	@Override
	public boolean check(Event e, Predicate<? super T> c) {
		return SimpleExpression.check(getAll(e), c, false, getAnd());
	}

	@Override
	public <R> Expression<? extends R> getConvertedExpression(Class<R>[] to) {
		if (StructCustomExpression.returnTypes.containsKey(which)
				&& !Converters.converterExists(StructCustomExpression.returnTypes.get(which), to)) {
			return null;
		}

		return new CustomExpression<>(this, to);
	}

	@Override
	public Class<T> getReturnType() {
		return superType;
	}

	@Override
	public boolean getAnd() {
		return true;
	}

	@Override
	public boolean setTime(int time) {
		return false;
	}

	@Override
	public int getTime() {
		return 0;
	}

	@Override
	public boolean isDefault() {
		return false;
	}

	@Override
	public Iterator<? extends T> iterator(Event e) {
		return new ArrayIterator<>(getAll(e));
	}

	@Override
	public boolean isLoopOf(String s) {
		return s.equalsIgnoreCase(StructCustomExpression.loopOfs.get(which));
	}

	@Override
	public Expression<?> getSource() {
		return source == null ? this : source;
	}

	@Override
	public Expression<? extends T> simplify() {
		return this;
	}

	@Override
	public String toString(Event e, boolean debug) {
		return which.getPattern();
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

	@Override
	public Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (StructCustomExpression.hasChanger.containsKey(which)
				&& StructCustomExpression.hasChanger.get(which).contains(mode)) {
			return StructCustomExpression.changerTypes
				.getOrDefault(which, Collections.emptyMap())
				.getOrDefault(mode, new Class[]{Object[].class});
		}

		return null;
	}

	@Override
	public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
		Trigger changer = StructCustomExpression.changerHandlers.getOrDefault(which, Collections.emptyMap()).get(mode);

		if (changer == null) {
			Skript.error(
				String.format("The custom expression '%s' no longer has a %s handler.",
					which.getPattern(), mode.name())
			);
		} else {
			ExpressionChangeEvent changeEvent =
				new ExpressionChangeEvent(e, exprs, which.getMatchedPattern(), parseResult, delta);
			SkriptReflection.putLocals(SkriptReflection.copyLocals(variablesMap), changeEvent);
			changer.execute(changeEvent);
		}
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
						SkriptParser.ParseResult parseResult) {
		// prevent the user from using the placeholder pattern we register in order to satisfy the registration requirements
		if (matchedPattern == 0) {
			return false;
		}

		which = StructCustomExpression.lookup(SkriptUtil.getCurrentScript(), matchedPattern);

		if (which == null) {
			return false;
		}

		if (which.shouldAdaptArgument()) {
			Expression<?> lastExpression = exprs[exprs.length - 1];
			System.arraycopy(exprs, 0, exprs, 1, exprs.length - 1);
			exprs[0] = lastExpression;
		}

		this.exprs = Arrays.stream(exprs)
			.map(SkriptUtil::defendExpression)
			.toArray(Expression[]::new);
		this.parseResult = parseResult;

		if (!SkriptUtil.canInitSafely(this.exprs)) {
			return false;
		}

		Class<?> returnType = StructCustomExpression.returnTypes.get(which);

		if (returnType != null) {
			this.types = new Class[]{returnType};
			this.superType = (Class<T>) returnType;
		}

		List<Supplier<Boolean>> suppliers = StructCustomExpression.usableSuppliers.get(which);
		if (suppliers != null && suppliers.size() != 0 && suppliers.stream().noneMatch(Supplier::get))
			return false;

		Boolean bool = StructCustomExpression.parseSectionLoaded.get(which);
		if (bool != null && !bool) {
			Skript.error("You can't use custom expressions with parse sections before they're loaded.");
			return false;
		}

		Trigger parseHandler = StructCustomExpression.parserHandlers.get(which);

		if (parseHandler != null) {
			SyntaxParseEvent event =
				new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, getParser().getCurrentEvents());

			TriggerItem.walk(parseHandler, event);
			variablesMap = SkriptReflection.removeLocals(event);

			return event.isMarkedContinue();
		}

		return true;
	}
}
