package com.btk5h.skriptmirror.util;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.DefaultClasses;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.script.Script;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class SkriptUtil {

	/**
	 * Returns the given {@link Expression}, unless it is (or has) an {@link UnparsedLiteral},
	 * in which case every {@link UnparsedLiteral} will be parsed, and returned if successful.
	 *
	 * Can also be used as an alternative to casting to a generic type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Expression<T> defendExpression(Expression<?> expr) {
		if (expr instanceof UnparsedLiteral) {
			Literal<?> parsed = ((UnparsedLiteral) expr).getConvertedExpression(Object.class);
			return (Expression<T>) (parsed == null ? expr : parsed);
		} else if (expr instanceof ExpressionList) {
			Expression<?>[] exprs = ((ExpressionList<?>) expr).getExpressions();
			for (int i = 0; i < exprs.length; i++) {
				exprs[i] = defendExpression(exprs[i]);
			}
		}
		return (Expression<T>) expr;
	}

	/**
	 * @return whether the given {@link Expression} is / has an {@link UnparsedLiteral}.
	 */
	public static boolean hasUnparsedLiteral(Expression<?> expr) {
		if (expr instanceof UnparsedLiteral)
			return true;

		if (expr instanceof ExpressionList) {
			Expression<?>[] expressions = ((ExpressionList<?>) expr).getExpressions();
			for (Expression<?> expression : expressions) {
				if (expression instanceof UnparsedLiteral)
					return true;
			}
		}

		return false;
	}

	/**
	 * @return whether the given {@link Expression}s contain any {@link UnparsedLiteral},
	 * checked using {@link #hasUnparsedLiteral(Expression)}.
	 */
	public static boolean canInitSafely(Expression<?>... expressions) {
		return Arrays.stream(expressions)
			.filter(Objects::nonNull)
			.noneMatch(SkriptUtil::hasUnparsedLiteral);
	}

	/**
	 * @return the {@link TriggerItem}s in the given {@link SectionNode},
	 * using {@link ScriptLoader#loadItems(SectionNode)}.
	 */
	public static List<TriggerItem> getItemsFromNode(SectionNode node) {
		RetainingLogHandler log = SkriptLogger.startRetainingLog();
		try {
			return ScriptLoader.loadItems(node);
		} finally {
			log.printLog();
			ParserInstance.get().deleteCurrentEvent();
		}
	}

	/**
	 * Deletes all the nodes in the given {@link SectionNode}.
	 */
	public static void clearSectionNode(SectionNode node) {
		List<Node> subNodes = new ArrayList<>();
		node.forEach(subNodes::add);
		subNodes.forEach(Node::remove);
	}

	/**
	 * {@return} the {@link ParserInstance#getCurrentScript()} as a {@link File}.
	 */
	public static File getCurrentScriptFile() {
		return Optional.ofNullable(getCurrentScript())
			.map(Script::getConfig)
			.map(Config::getFile)
			.orElse(null);
	}

	public static Script getCurrentScript() {
		return Optional.of(ParserInstance.get())
			.filter(ParserInstance::isActive)
			.map(ParserInstance::getCurrentScript)
			.orElse(null);
	}

	/**
	 * Gets the {@link ClassInfo} by first converting the given string to a singular.
	 * Returns {@code Object.class}'s if no {@link ClassInfo} can be found for the given type.
	 */
	@NotNull
	public static ClassInfo<?> getUserClassInfo(String name) {
		NonNullPair<String, Boolean> wordData = Utils.getEnglishPlural(name);

		ClassInfo<?> ci = Classes.getClassInfoNoError(wordData.getFirst());

		if (ci == null) {
			ci = Classes.getClassInfoFromUserInput(wordData.getFirst());
		}

		if (ci == null) {
			Skript.warning(String.format("'%s' is not a valid Skript type. Using 'object' instead.", name));
			return DefaultClasses.OBJECT;
		}

		return ci;
	}

	/**
	 * @return the pair of the {@link ClassInfo} in the given string, and whether is is singular.
	 */
	public static NonNullPair<ClassInfo<?>, Boolean> getUserClassInfoAndPlural(String name) {
		NonNullPair<String, Boolean> wordData = Utils.getEnglishPlural(name);
		ClassInfo<?> ci = getUserClassInfo(name);

		return new NonNullPair<>(ci, wordData.getSecond());
	}

	/**
	 * @return the singular form of the given string type,
	 * converted back to plural if it was plural in the first place.
	 * This causes the string type to always be the default codename for the {@link ClassInfo}.
	 */
	public static String replaceUserInputPatterns(String name) {
		NonNullPair<String, Boolean> wordData = Utils.getEnglishPlural(name);
		ClassInfo<?> ci = getUserClassInfo(name);

		return Utils.toEnglishPlural(ci.getCodeName(), wordData.getSecond());
	}

	/**
	 * {@return} a {@link Function} to get a {@link Expression}'s value,
	 * using {@link Expression#getSingle(Event)} if {@link Expression#getSingle(Event)}
	 * returns {@code true}, otherwise returning {@link Expression#getArray(Event)}.
	 */
	public static Function<Expression<?>, Object> unwrapWithEvent(Event e) {
		return expr -> expr.isSingle() ? expr.getSingle(e) : expr.getArray(e);
	}

}
