package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.*;

public class CustomEffect {
  static {
    //noinspection unchecked
    Skript.registerEvent("*Define Effect", CustomEffect.EventHandler.class, EffectEvent.class,
        "effect <.+>");

    Skript.registerEffect(EffectHandler.class);
    Optional<SyntaxElementInfo<? extends Effect>> info = Skript.getEffects().stream()
        .filter(i -> i.c == EffectHandler.class)
        .findFirst();

    if (info.isPresent()) {
      thisInfo = info.get();
    } else {
      Skript.warning("Could not find custom effect class. Custom effects will not work.");
    }
  }

  private static SyntaxElementInfo<?> thisInfo;
  private static final SectionValidator EFFECT_DECLARATION = new SectionValidator()
      .addSection("trigger", false);

  public static class EffectEvent extends CustomSyntaxEvent {
    private final static HandlerList handlers = new HandlerList();
    private final String which;
    private final TriggerItem next;
    private boolean sync = true;

    public EffectEvent(Event event, Expression<?>[] expressions,
                       SkriptParser.ParseResult parseResult, String which, TriggerItem next) {
      super(event, expressions, parseResult);
      this.which = which;
      this.next = next;
    }

    public String getWhich() {
      return which;
    }

    public TriggerItem getNext() {
      return next;
    }

    public boolean isSync() {
      return sync;
    }

    public void setSync(boolean sync) {
      this.sync = sync;
    }

    public static HandlerList getHandlerList() {
      return handlers;
    }

    @Override
    public HandlerList getHandlers() {
      return handlers;
    }
  }

  private static List<String> effects = new ArrayList<>();
  private static Map<String, Trigger> effectHandlers = new HashMap<>();

  private static void updateEffects() {
    Util.setPatterns(thisInfo, effects.toArray(new String[0]));
  }

  public static class EventHandler extends SelfRegisteringSkriptEvent {
    private String which;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern,
                        SkriptParser.ParseResult parseResult) {
      which = Util.preprocessPattern(parseResult.regexes.get(0).group());
      if (effects.contains(which)) {
        Skript.error(String.format("The custom effect '%s' already has a handler.", which));
        return false;
      }
      effects.add(which);

      SectionNode node = (SectionNode) SkriptLogger.getNode();
      node.convertToEntries(0);

      boolean ok = EFFECT_DECLARATION.validate(node);

      if (!ok) {
        unregister(null);
        return false;
      }

      register(node);
      return true;
    }

    @SuppressWarnings("unchecked")
    private void register(SectionNode node) {
      ScriptLoader.setCurrentEvent("custom efffect trigger", EffectEvent.class);
      Util.getItemsFromNode(node, "trigger").ifPresent(items ->
        effectHandlers.put(which, new Trigger(ScriptLoader.currentScript.getFile(), "effect " + which, this, items))
      );

      Util.clearSectionNode(node);
      updateEffects();
    }

    @Override
    public void register(Trigger t) {
    }

    @Override
    public void unregister(Trigger t) {
      effects.remove(which);
      effectHandlers.remove(which);
      updateEffects();
    }

    @Override
    public void unregisterAll() {
      effects.clear();
      effectHandlers.clear();
      updateEffects();
    }

    @Override
    public String toString(Event e, boolean debug) {
      return which;
    }
  }

  public static class EffectHandler extends Effect {
    private String which;
    private Expression<?>[] exprs;
    private SkriptParser.ParseResult parseResult;

    @Override
    protected void execute(Event e) {
      // for effect commands
      invokeEffect(e);
    }

    @Override
    protected TriggerItem walk(Event e) {
      EffectEvent effectEvent = invokeEffect(e);

      if (effectEvent.isSync()) {
        return getNext();
      }
      return null;
    }

    private EffectEvent invokeEffect(Event e) {
      Trigger trigger = effectHandlers.get(which);
      EffectEvent effectEvent = new EffectEvent(e, exprs, parseResult, which, getNext());
      if (trigger == null) {
        Skript.error(String.format("The custom effect '%s' no longer has a handler.", which));
      } else {
        trigger.execute(effectEvent);
      }
      return effectEvent;
    }

    @Override
    public String toString(Event e, boolean debug) {
      return which;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
      which = effects.get(matchedPattern);
      this.exprs = Arrays.stream(exprs)
          .map(Util::defendExpression)
          .toArray(Expression[]::new);
      this.parseResult = parseResult;
      return Util.canInitSafely(this.exprs);
    }
  }
}
