package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.*;
import java.util.stream.StreamSupport;

public class CustomExpression {
  static {
    //noinspection unchecked
    Skript.registerEvent("*Define Expression", CustomExpression.EventHandler.class,
        new Class[]{ExpressionGetEvent.class, ExpressionChangeEvent.class},
        "[(1¦(plural|non(-|[ ])single|multi[ple]))] expression <.+>",
        "%*classinfo% property <.+>");

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
  private static final SectionValidator EXPRESSION_DECLARATION;

  static {
    EXPRESSION_DECLARATION =
        new SectionValidator()
            .addEntry("return type", true)
            .addSection("get", true);
    Arrays.stream(Changer.ChangeMode.values())
        .map(mode -> mode.name().replace("_", " ").toLowerCase())
        .forEach(mode -> EXPRESSION_DECLARATION.addSection(mode, true));
  }

  private static class SyntaxInfo {
    private final String pattern;
    private final int[] inheritedSingles;
    private final boolean alwaysPlural;
    private final boolean adaptArgument;
    private final boolean property;


    private SyntaxInfo(String pattern, int[] inheritedSingles, boolean alwaysPlural,
                       boolean adaptArgument, boolean property) {
      this.pattern = pattern;
      this.inheritedSingles = inheritedSingles;
      this.alwaysPlural = alwaysPlural;
      this.adaptArgument = adaptArgument;
      this.property = property;
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

    public boolean isProperty() {
      return property;
    }

    @Override
    public String toString() {
      return String.format("%s (singles: %s, plural: %s, adapt: %s, property: %s)",
          pattern, Arrays.toString(inheritedSingles), alwaysPlural, adaptArgument, property);
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

    public ExpressionChangeEvent(Event event, Expression<?>[] expressions,
                                 SkriptParser.ParseResult parseResult, Object[] delta) {
      super(event, expressions, parseResult);
      this.delta = delta;
    }

    public static HandlerList getHandlerList() {
      return handlers;
    }

    public Object[] getDelta() {
      return delta;
    }

    @Override
    public HandlerList getHandlers() {
      return handlers;
    }
  }

  private static List<String> expressions = new ArrayList<>();
  private static Map<String, SyntaxInfo> expressionInfos = new HashMap<>();
  private static Map<SyntaxInfo, Class<?>> returnTypes = new HashMap<>();
  private static Map<SyntaxInfo, Trigger> expressionHandlers = new HashMap<>();
  private static Map<SyntaxInfo, Map<Changer.ChangeMode, Trigger>> changerHandlers =
      new HashMap<>();

  private static void updateExpressions() {
    Util.setPatterns(thisInfo, expressions.toArray(new String[0]));
  }

  public static class EventHandler extends SelfRegisteringSkriptEvent {
    private List<SyntaxInfo> whiches = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern,
                        SkriptParser.ParseResult parseResult) {
      String what = parseResult.regexes.get(0).group();
      switch (matchedPattern) {
        case 0:
          whiches.add(createSyntaxInfo(what, (parseResult.mark & 1) == 1, false, false));
          break;
        case 1:
          String fromType = ((Literal<ClassInfo>) args[0]).getSingle().getCodeName();
          whiches.add(createSyntaxInfo("[the] " + what + " of %$" + fromType + "s%", false, true, true));
          whiches.add(createSyntaxInfo("%$" + fromType + "s%'[s] " + what, false, false, true));
          break;
      }

      whiches.forEach(which -> {
        String pattern = which.getPattern();
        if (!expressions.contains(pattern)) {
          if (expressions.contains(pattern)) {
            Skript.error(String.format("The custom expression '%s' already has a handler.",
                pattern));
          } else {
            expressions.add(pattern);
            expressionInfos.put(pattern, which);
          }
        }
      });

      SectionNode node = (SectionNode) SkriptLogger.getNode();
      node.convertToEntries(0);

      boolean ok = EXPRESSION_DECLARATION.validate(node);

      if (!ok) {
        unregister(null);
        return false;
      }

      register(node);

      return true;
    }

    @SuppressWarnings("unchecked")
    private void register(SectionNode node) {
      String userReturnType = node.getValue("return type");
      if (userReturnType != null) {
        Class returnType =
            Classes.getClassFromUserInput(ScriptLoader.replaceOptions(userReturnType));
        whiches.forEach(which -> returnTypes.put(which, returnType));
      }

      ScriptLoader.setCurrentEvent("custom expression getter", ExpressionGetEvent.class);
      Util.getItemsFromNode(node, "get").ifPresent(items ->
          whiches.forEach(which ->
              expressionHandlers.put(which,
                  new Trigger(ScriptLoader.currentScript.getFile(), "get " + which.getPattern(),
                      this, items))
          )
      );

      Arrays.stream(Changer.ChangeMode.values())
          .forEach(mode -> {
            String name = mode.name().replace("_", " ").toLowerCase();
            ScriptLoader.setCurrentEvent("custom expression changer", ExpressionChangeEvent.class);
            Util.getItemsFromNode(node, name).ifPresent(items ->
                whiches.forEach(which -> {
                      Map<Changer.ChangeMode, Trigger> changerMap =
                          changerHandlers.computeIfAbsent(which, k -> new HashMap<>());
                      changerMap.put(mode,
                          new Trigger(ScriptLoader.currentScript.getFile(),
                              String.format("%s %s", name, which.getPattern()), this, items));
                    }
                )
            );
          });

      Util.clearSectionNode(node);
      updateExpressions();
    }

    @Override
    public void register(Trigger t) {
    }

    @Override
    public void unregister(Trigger t) {
      whiches.forEach(which -> {
        expressionHandlers.remove(which);
        changerHandlers.remove(which);
        returnTypes.remove(which);
        expressions.remove(which.getPattern());
        expressionInfos.remove(which.getPattern());
      });
      updateExpressions();
    }

    @Override
    public void unregisterAll() {
      expressions.clear();
      expressionInfos.clear();
      returnTypes.clear();
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
      Trigger getter = expressionHandlers.get(which);

      if (getter == null) {
        Skript.error(
            String.format("The custom expression '%s' no longer has a get handler.",
                which.getPattern())
        );
        return Util.newArray(superType, 0);
      }

      if (which.isProperty()) {
        return getByProperty(e, getter);
      }

      return getByStandard(e, getter);
    }

    private T[] getByStandard(Event e, Trigger getter) {
      ExpressionGetEvent expressionEvent = new ExpressionGetEvent(e, exprs, parseResult);
      getter.execute(expressionEvent);
      if (expressionEvent.getOutput() == null) {
        Skript.error(
            String.format("The get handler for '%s' did not return.", which.getPattern())
        );
        return Util.newArray(superType, 0);
      }

      return Converters.convertArray(expressionEvent.getOutput(), types, superType);
    }

    private T[] getByProperty(Event e, Trigger getter) {
      List<T> output = new ArrayList<>();
      for (Object o : exprs[0].getArray(e)) {
        Expression<?>[] localExprs = Arrays.copyOf(exprs, exprs.length);
        localExprs[0] = new SimpleLiteral<>(o, false);

        ExpressionGetEvent expressionEvent = new ExpressionGetEvent(e, localExprs, parseResult);
        getter.execute(expressionEvent);

        Object[] exprOutput = expressionEvent.getOutput();
        if (exprOutput == null) {
          Skript.error(
              String.format("The get handler for '%s' did not return.", which.getPattern())
          );
          return Util.newArray(superType, 0);
        }

        if (exprOutput.length > 1) {
          Skript.error(
              String.format("The get handler for '%s' returned more than one value.", which.getPattern())
          );
          return Util.newArray(superType, 0);
        }

        if (exprOutput.length == 1) {
          output.add(Converters.convert(exprOutput[0], superType));
        }
      }

      return output.toArray(Util.newArray(superType, 0));
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
      if (returnTypes.containsKey(which)
          && !Converters.converterExists(returnTypes.get(which), to)) {
        return null;
      }

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
    public String toString() {
      return toString(null, false);
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
      return changerHandlers.containsKey(which) && changerHandlers.get(which).containsKey(mode)
          ? new Class[]{Object[].class} : null;
    }

    @Override
    public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
      Trigger changer = changerHandlers.getOrDefault(which, Collections.emptyMap()).get(mode);

      if (changer == null) {
        Skript.error(
            String.format("The custom expression '%s' no longer has a %s handler.",
                which.getPattern(), mode.name())
        );
      } else {
        changer.execute(new ExpressionChangeEvent(e, exprs, parseResult, delta));
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

      this.exprs = Arrays.stream(exprs)
          .map(Util::defendExpression)
          .toArray(Expression[]::new);
      this.parseResult = parseResult;
      return Util.canInitSafely(this.exprs);
    }
  }

  private static SyntaxInfo createSyntaxInfo(String pattern, boolean alwaysPlural,
                                             boolean adaptArgument, boolean property) {
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

        if (part.startsWith("_")) {
          part = part.endsWith("s") ? "objects" : "object";
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
        adaptArgument,
        property);
  }
}
