package com.btk5h.skriptmirror.skript.custom;

import com.btk5h.skriptmirror.Util;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SelfRegisteringSkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;

public class CustomExpression {
  static {
    //noinspection unchecked
    Skript.registerEvent("*Define Expression", CustomExpression.EventHandler.class,
        new Class[]{ExpressionGetEvent.class, ExpressionChangeEvent.class},
        "(get|1¦change) [(2¦(plural|non(-|[ ])single|multi[ple]))] expression <.+>",
        "(get|1¦change) [(2¦(plural|non(-|[ ])single|multi[ple]))] %*classinfo% property <.+>");

    //noinspection unchecked
    Skript.registerExpression(ExpressionHandler.class, Object.class,
        ExpressionType.PATTERN_MATCHES_EVERYTHING);
    Optional<ExpressionInfo<?, ?>> info = StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(Skript.getExpressions(), Spliterator.ORDERED), false)
        .filter(i -> i.c == ExpressionHandler.class)
        .findFirst();

    if (info.isPresent()) {
      thisInfo = info.get();
    } else {
      Skript.warning("Could not find custom expression class. Custom expressions will not work.");
    }
  }

  private static SyntaxElementInfo<?> thisInfo;

  private static class SyntaxInfo {
    private final String pattern;
    private final int[] inheritedSingles;
    private final boolean alwaysPlural;
    private final boolean adaptArgument;


    private SyntaxInfo(String pattern, int[] inheritedSingles, boolean alwaysPlural,
                       boolean adaptArgument) {
      this.pattern = pattern;
      this.inheritedSingles = inheritedSingles;
      this.alwaysPlural = alwaysPlural;
      this.adaptArgument = adaptArgument;
    }

    public String getPattern() {
      return pattern;
    }

    public int[] getInheritedSingles() {
      return inheritedSingles;
    }

    public boolean isAlwaysPlural() {
      return alwaysPlural;
    }

    public boolean shouldAdaptArgument() {
      return adaptArgument;
    }

    @Override
    public String toString() {
      return String.format("%s (singles: %s, plural: %s, adapt: %s)",
          pattern, Arrays.toString(inheritedSingles), alwaysPlural, adaptArgument);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SyntaxInfo that = (SyntaxInfo) o;
      return alwaysPlural == that.alwaysPlural &&
          adaptArgument == that.adaptArgument &&
          Objects.equals(pattern, that.pattern) &&
          Arrays.equals(inheritedSingles, that.inheritedSingles);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(pattern, alwaysPlural, adaptArgument);
      result = 31 * result + Arrays.hashCode(inheritedSingles);
      return result;
    }
  }

  public static class ExpressionGetEvent extends CustomSyntaxEvent {
    private final static HandlerList handlers = new HandlerList();
    private Object[] output;

    public ExpressionGetEvent(Event event, Expression<?>[] expressions,
                              SkriptParser.ParseResult parseResult) {
      super(event, expressions, parseResult);
    }

    public static HandlerList getHandlerList() {
      return handlers;
    }

    public Object[] getOutput() {
      return output;
    }

    public void setOutput(Object[] output) {
      this.output = output;
    }

    @Override
    public HandlerList getHandlers() {
      return handlers;
    }
  }

  public static class ExpressionChangeEvent extends CustomSyntaxEvent {
    private final static HandlerList handlers = new HandlerList();
    private final Object[] delta;
    private final Changer.ChangeMode mode;

    public ExpressionChangeEvent(Event event, Expression<?>[] expressions,
                                 SkriptParser.ParseResult parseResult, Object[] delta,
                                 Changer.ChangeMode mode) {
      super(event, expressions, parseResult);
      this.delta = delta;
      this.mode = mode;
    }

    public static HandlerList getHandlerList() {
      return handlers;
    }

    public Object[] getDelta() {
      return delta;
    }

    public Changer.ChangeMode getMode() {
      return mode;
    }

    @Override
    public HandlerList getHandlers() {
      return handlers;
    }
  }

  private static List<String> expressions = new ArrayList<>();
  private static Map<String, SyntaxInfo> expressionInfos = new HashMap<>();
  private static Map<SyntaxInfo, Trigger> expressionHandlers = new HashMap<>();
  private static Map<SyntaxInfo, Trigger> changerHandlers = new HashMap<>();

  private static void updateExpressions() {
    Util.setPatterns(thisInfo, expressions.toArray(new String[0]));
  }

  public static class EventHandler extends SelfRegisteringSkriptEvent {
    private List<SyntaxInfo> whiches = new ArrayList<>();
    private boolean isChanger;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern,
                        SkriptParser.ParseResult parseResult) {
      isChanger = (parseResult.mark & 1) == 1;

      String what = parseResult.regexes.get(0).group();
      switch (matchedPattern) {
        case 0:
          whiches.add(createSyntaxInfo(what, (parseResult.mark & 2) == 2, false));
          break;
        case 1:
          String fromType = ((Literal<ClassInfo>) args[0]).getSingle().getCodeName();
          whiches.add(createSyntaxInfo("[the] " + what + " of %$" + fromType + "s%", false, true));
          whiches.add(createSyntaxInfo("%$" + fromType + "s%'[s] " + what, false, false));
          break;
      }

      return true;
    }

    @Override
    public void register(Trigger t) {
      whiches.forEach(which -> {
        String pattern = which.getPattern();
        if (!expressions.contains(pattern)) {
          expressions.add(pattern);
          expressionInfos.put(pattern, which);
        }

        Map<SyntaxInfo, Trigger> handlerMap = isChanger ? changerHandlers : expressionHandlers;
        if (handlerMap.containsKey(which)) {
          Skript.error(String.format("The custom expression '%s' already has a handler.", pattern));
        } else {
          handlerMap.put(which, t);
        }
      });
      updateExpressions();
    }

    @Override
    public void unregister(Trigger t) {
      whiches.forEach(which -> {
        Map<SyntaxInfo, Trigger> handlerMap = isChanger ? changerHandlers : expressionHandlers;
        handlerMap.remove(which);

        if (!expressionHandlers.containsKey(which) && !changerHandlers.containsKey(which)) {
          expressions.remove(which.getPattern());
          expressionInfos.remove(which.getPattern());
        }
      });
      updateExpressions();
    }

    @Override
    public void unregisterAll() {
      expressions.clear();
      expressionInfos.clear();
      expressionHandlers.clear();
      changerHandlers.clear();
      updateExpressions();
    }

    @Override
    public String toString(Event e, boolean debug) {
      return "expression: " + whiches.toString();
    }
  }

  public static class ExpressionHandler<T> implements Expression<T> {
    private SyntaxInfo which;
    private Expression<?>[] exprs;
    private SkriptParser.ParseResult parseResult;

    private final ExpressionHandler<?> source;
    private final Class<? extends T>[] types;
    private final Class<T> superType;

    @SuppressWarnings("unchecked")
    public ExpressionHandler() {
      this(null, (Class<? extends T>) Object.class);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private ExpressionHandler(ExpressionHandler<?> source, Class<? extends T>... types) {
      this.source = source;

      if (source != null) {
        this.which = source.which;
        this.exprs = source.exprs;
        this.parseResult = source.parseResult;
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
      Trigger trigger = expressionHandlers.get(which);
      ExpressionGetEvent expressionEvent = new ExpressionGetEvent(e, exprs, parseResult);

      if (trigger == null) {
        Skript.error(
            String.format("The custom expression '%s' no longer has a get handler.",
                which.getPattern())
        );
        return Util.newArray(superType, 0);
      } else {
        trigger.execute(expressionEvent);
      }

      if (expressionEvent.getOutput() == null) {
        Skript.error(
            String.format("The get handler for '%s' did not continue.", which.getPattern())
        );
        return Util.newArray(superType, 0);
      }

      return Converters.convertArray(expressionEvent.getOutput(), types, superType);
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
    public boolean check(Event e, Checker<? super T> c, boolean negated) {
      return SimpleExpression.check(getAll(e), c, negated, getAnd());
    }

    @Override
    public boolean check(Event e, Checker<? super T> c) {
      return SimpleExpression.check(getAll(e), c, false, getAnd());
    }

    @Override
    public <R> Expression<? extends R> getConvertedExpression(Class<R>[] to) {
      return new ExpressionHandler<>(this, to);
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
      return false;
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
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
      return changerHandlers.containsKey(which) ? new Class[]{Object[].class} : null;
    }

    @Override
    public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
      Trigger trigger = changerHandlers.get(which);
      ExpressionChangeEvent expressionEvent =
          new ExpressionChangeEvent(e, exprs, parseResult, delta, mode);

      if (trigger == null) {
        Skript.error(
            String.format("The custom expression '%s' no longer has a change handler.",
                which.getPattern())
        );
      } else {
        trigger.execute(expressionEvent);
      }
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
      String pattern = expressions.get(matchedPattern);
      which = expressionInfos.get(pattern);

      if (which.shouldAdaptArgument()) {
        Expression<?> lastExpression = exprs[exprs.length - 1];
        System.arraycopy(exprs, 0, exprs, 1, exprs.length - 1);
        exprs[0] = lastExpression;
      }

      this.exprs = exprs;
      this.parseResult = parseResult;
      return Arrays.stream(exprs).noneMatch(expr -> expr instanceof UnparsedLiteral);
    }
  }

  private static SyntaxInfo createSyntaxInfo(String pattern, boolean alwaysPlural,
                                             boolean adaptArgument) {
    StringBuilder newPattern = new StringBuilder(pattern.length());
    List<Integer> inheritedSingles = new ArrayList<>();
    String[] parts = pattern.split("%");

    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (i % 2 == 0) {
        newPattern.append(part);
      } else {
        if (part.startsWith("$")) {
          part = part.substring(1);
          inheritedSingles.add(i / 2);
        }
        newPattern.append('%');
        newPattern.append(part);
        newPattern.append('%');
      }
    }

    return new SyntaxInfo(
        newPattern.toString(),
        inheritedSingles.stream()
            .mapToInt(i -> i)
            .toArray(),
        alwaysPlural,
        adaptArgument
    );
  }
}
