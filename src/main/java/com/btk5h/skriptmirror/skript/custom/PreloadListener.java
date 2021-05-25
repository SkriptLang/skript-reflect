package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.events.bukkit.PreScriptLoadEvent;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.custom.condition.CustomConditionSection;
import com.btk5h.skriptmirror.skript.custom.effect.CustomEffectSection;
import com.btk5h.skriptmirror.skript.custom.event.CustomEventSection;
import com.btk5h.skriptmirror.skript.custom.expression.CustomExpressionSection;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreloadListener implements Listener {

  private static final Map<String, Class<? extends PreloadableEvent>> keywordMap = new HashMap<>();
  private static final Map<Class<? extends PreloadableEvent>, List<String>> syntaxMap = new HashMap<>();

  private static boolean preloadEnabled = false;

  static {
    Bukkit.getPluginManager().registerEvents(new PreloadListener(), SkriptMirror.getInstance());

    keywordMap.put("condition", CustomConditionSection.class);
    keywordMap.put("effect", CustomEffectSection.class);
    keywordMap.put("expression", CustomExpressionSection.class);
    keywordMap.put("property", CustomExpressionSection.class);
    keywordMap.put("event", CustomEventSection.class);

    if (SkriptMirror.getInstance().getConfig().getBoolean("enable-preloading")) {
      try {
        // Check for older Skript versions
        PreScriptLoadEvent.class.getDeclaredField("scripts");
        preloadEnabled = true;
      } catch (NoSuchFieldException ignored) {
      }
    }
  }

  @EventHandler
  public void onPreScriptLoad(PreScriptLoadEvent preScriptLoadEvent) {
    if (!preloadEnabled)
      return;

    List<Config> scripts = preScriptLoadEvent.getScripts();
    for (Config script : scripts) {
      SkriptReflection.setCurrentScript(script);
      for (Node node : script.getMainNode()) {
        if (node instanceof SectionNode) {
          handleEventNode((SectionNode) node);
        }
      }
    }
  }

  public static void handleEventNode(SectionNode sectionNode) {
    String key = sectionNode.getKey();
    assert key != null;
    for (Map.Entry<String, Class<? extends PreloadableEvent>> entry : keywordMap.entrySet()) {
      Class<? extends PreloadableEvent> preloadableEventClass = entry.getValue();
      if (containsWord(key, entry.getKey())) {

        SkriptLogger.setNode(sectionNode);
        ParseLogHandler parseLogHandler = SkriptLogger.startParseLogHandler();

        try {
          List<String> patternList = syntaxMap.get(preloadableEventClass);
          for (int i = 0; i < patternList.size(); i++) {
            String pattern = patternList.get(i);

            try {
              SkriptParser.ParseResult parseResult = SkriptReflection.parse_i(
                new SkriptParser(key, SkriptParser.PARSE_LITERALS, ParseContext.EVENT), pattern, 0, 0);
              if (parseResult != null) {

                PreloadableEvent preloadableEvent = preloadableEventClass.newInstance();
                Literal<?>[] literals = Arrays.copyOf(parseResult.exprs, parseResult.exprs.length, Literal[].class);

                if (!preloadableEvent.init(literals, i, parseResult, true)) {
                  parseLogHandler.printError();
                }
                parseLogHandler.printLog();

                return;
              }
            } catch (IllegalAccessException | InstantiationException e) {
              assert false;
            }

          }
        } finally {
          parseLogHandler.stop();
        }

      }
    }
  }

  public static boolean containsWord(String string, String query) {
    return string.equals(query) || string.startsWith(query + " ") || string.endsWith(" " + query) ||
      string.contains(" " + query + " ");
  }

  public static void addSyntax(Class<? extends PreloadableEvent> customSyntaxClass, String... strings) {
    syntaxMap
      .computeIfAbsent(customSyntaxClass, (key) -> new ArrayList<>())
      .addAll(Arrays.asList(strings));
  }

}
