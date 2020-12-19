package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.*;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.skript.custom.event.BukkitCustomEvent;
import com.btk5h.skriptmirror.skript.custom.event.CustomEvent;
import com.btk5h.skriptmirror.skript.custom.event.CustomEventUtils;
import com.btk5h.skriptmirror.skript.custom.event.EventSyntaxInfo;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class CustomSyntaxSection<T extends CustomSyntaxSection.SyntaxData>
    extends SelfRegisteringSkriptEvent implements PreloadableEvent {

  @SuppressWarnings("unchecked")
  public static <E extends SkriptEvent> SkriptEventInfo<E> register(String name, Class<E> c, String... patterns) {
    return Skript.registerEvent("*" + name, c, new Class[0], patterns);
  }

  public static class DataTracker<T> {
    public DataTracker() {
    }

    private List<String> patterns = new ArrayList<>();
    private final Map<File, Map<String, T>> primaryData = new HashMap<>();
    private final List<Map<T, ?>> managedData = new ArrayList<>();
    private SyntaxElementInfo<?> info;

    public List<String> getPatterns() {
      return patterns;
    }

    public Map<File, Map<String, T>> getPrimaryData() {
      return primaryData;
    }

    public List<Map<T, ?>> getManagedData() {
      return managedData;
    }

    public SyntaxElementInfo<?> getInfo() {
      return info;
    }

    public void recomputePatterns() {
      patterns = primaryData.values().stream()
          .map(Map::keySet)
          .flatMap(Set::stream)
          .distinct()
          .collect(Collectors.toList());
    }

    public void addManaged(Map<T, ?> data) {
      managedData.add(data);
    }

    public void setInfo(SyntaxElementInfo<?> info) {
      this.info = info;
    }

    public final T lookup(File script, int matchedPattern) {
      String originalSyntax = patterns.get(matchedPattern);
      Map<String, T> localSyntax = primaryData.get(script);

      if (localSyntax != null) {
        T privateResult = localSyntax.get(originalSyntax);
        if (privateResult != null) {
          return privateResult;
        }
      }

      Map<String, T> globalSyntax = primaryData.get(null);

      if (globalSyntax == null || !globalSyntax.containsKey(originalSyntax)) {
        return null;
      }

      return globalSyntax.get(originalSyntax);
    }
  }

  public abstract static class SyntaxData {
    private final File script;
    private final String pattern;
    private final int matchedPattern;

    protected SyntaxData(File script, String pattern, int matchedPattern) {
      this.script = script;
      this.pattern = pattern;
      this.matchedPattern = matchedPattern;
    }

    public File getScript() {
      return script;
    }

    public String getPattern() {
      return pattern;
    }

    public int getMatchedPattern() {
      return matchedPattern;
    }

    @Override
    public String toString() {
      return pattern;
    }
  }

  private static final Map<SectionNode, CustomSyntaxSection<?>> preloadStorage = new HashMap<>();

  protected List<T> whichInfo = new ArrayList<>();

  protected boolean isPreloaded;

  private boolean preloadSuccess;

  private RetainingLogHandler preloadLogHandler;

  protected abstract DataTracker<T> getDataTracker();

  @Override
  public void register(Trigger t) {
  }

  @Override
  public void unregister(Trigger t) {
    whichInfo.forEach(which -> {
      Map<File, Map<String, T>> primaryData = getDataTracker().getPrimaryData();
      File script = which.getScript();

      Map<String, T> syntaxes = primaryData.get(script);
      syntaxes.remove(which.getPattern());
      if (syntaxes.size() == 0) {
        primaryData.remove(script);
      }

      getDataTracker().getManagedData().forEach(data -> data.remove(which));
    });

    update();
  }

  @Override
  public void unregisterAll() {
    getDataTracker().getPatterns().clear();
    getDataTracker().getPrimaryData().clear();
    getDataTracker().getManagedData().forEach(Map::clear);

    update();
  }

  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
    return init(args, matchedPattern, parseResult, false);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, boolean isPreload) {
    SectionNode node = (SectionNode) SkriptLogger.getNode();
    CustomSyntaxSection<T> customSyntaxSection = (CustomSyntaxSection<T>) preloadStorage.get(node);
    if (customSyntaxSection != null) {
      this.whichInfo = customSyntaxSection.whichInfo;
      this.isPreloaded = customSyntaxSection.isPreloaded;
      this.preloadSuccess = customSyntaxSection.preloadSuccess;
      this.preloadLogHandler = customSyntaxSection.preloadLogHandler;
      preloadStorage.remove(node);
    } else if (isPreload) {
      preloadStorage.put(node, this);
    }

    if (isPreloaded && !preloadSuccess) {
      preloadLogHandler.printLog();
      return false;
    }

    node.convertToEntries(0);

    if (node.getKey().toLowerCase().startsWith("on ")) {
      return false;
    }

    if (isPreload)
      preloadLogHandler = SkriptLogger.startRetainingLog();
    preloadSuccess = init(args, matchedPattern, parseResult, node, isPreload);
    if (isPreload) {
      preloadLogHandler.stop();
    } else if (preloadLogHandler != null) {
      preloadLogHandler.printLog();
    }


    if (isPreload)
      isPreloaded = true;

    if (!isPreload)
      SkriptUtil.clearSectionNode(node);

    if (!preloadSuccess) {
      unregister(null);
    }

    return preloadSuccess;
  }

  protected abstract boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                                  SectionNode node, boolean isPreload);

  protected static boolean handleEntriesAndSections(SectionNode node,
                                                    Predicate<EntryNode> entryHandler,
                                                    Predicate<SectionNode> sectionHandler) {
    boolean ok = true;

    for (Node subNode : SkriptReflection.getNodes(node)) {
      SkriptLogger.setNode(subNode);

      if (subNode instanceof EntryNode) {
        if (!entryHandler.test(((EntryNode) subNode))) {
          Skript.error(String.format("Unexpected entry '%s'. Check whether it's spelled correctly or remove it.",
              subNode.getKey()));
          ok = false;
        }
      } else if (subNode instanceof SectionNode) {
        if (!sectionHandler.test(((SectionNode) subNode))) {
          Skript.error(String.format("Unexpected section '%s'. Check whether it's spelled correctly or remove it.",
              subNode.getKey()));
          ok = false;
        }
      } else if (subNode instanceof InvalidNode || !(subNode instanceof VoidNode )) {
        ok = false;
      }

      SkriptLogger.setNode(null);
    }

    return ok;
  }

  @Override
  public String toString(Event e, boolean debug) {
    return null;
  }

  private void update() {
    getDataTracker().recomputePatterns();
    SkriptReflection.setPatterns(getDataTracker().getInfo(), getDataTracker().getPatterns().toArray(new String[0]));
  }

  protected final void register(T data) {
    String pattern = data.getPattern();

    whichInfo.add(data);

    getDataTracker().getPrimaryData()
        .computeIfAbsent(data.getScript(), f -> new HashMap<>())
        .put(pattern, data);

    update();
  }

  @SuppressWarnings("unchecked")
  protected boolean handleUsableSection(SectionNode sectionNode, Map<T, List<Supplier<Boolean>>> usableSuppliers) {
    File currentScript = SkriptUtil.getCurrentScript();
    for (Node usableNode : sectionNode) {
      String usableKey = usableNode.getKey();
      assert usableKey != null;

      Supplier<Boolean> supplier;
      if (usableKey.startsWith("custom event ")) {
        String customEventString = usableKey.substring("custom event ".length());
        VariableString variableString = VariableString.newInstance(
          customEventString.substring(1, customEventString.length() - 1));
        if (variableString == null || !variableString.isSimple()) {
          Skript.error("Custom event identifiers may only be simple strings");
          return false;
        } else {
          String identifier = variableString.toString(null);
          supplier = () -> {
            if (!ScriptLoader.isCurrentEvent(BukkitCustomEvent.class))
              return false;

            EventSyntaxInfo eventWhich = CustomEvent.lastWhich;
            return CustomEventUtils.getName(eventWhich).equalsIgnoreCase(identifier);
          };
        }
      } else {
        JavaType javaType = CustomImport.lookup(currentScript, usableKey);
        Class<?> javaClass = javaType == null ? null : javaType.getJavaClass();
        if (javaClass == null || !Event.class.isAssignableFrom(javaClass)) {
          Skript.error(javaType + " is not a Bukkit event");
          return false;
        }
        Class<? extends Event> eventClass = (Class<? extends Event>) javaClass;

        supplier = () -> ScriptLoader.isCurrentEvent(eventClass);
      }
      whichInfo.forEach(which ->
        usableSuppliers.computeIfAbsent(which, (whichIndex) -> new ArrayList<>())
          .add(supplier)
      );
    }
    return true;
  }

}
