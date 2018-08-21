package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class CustomSyntaxSection<T extends CustomSyntaxSection.SyntaxData>
    extends SelfRegisteringSkriptEvent {
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
    private String syntaxType = "syntax";
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

    public String getSyntaxType() {
      return syntaxType;
    }

    public SyntaxElementInfo<?> getInfo() {
      return info;
    }

    public void recomputePatterns() {
      patterns = primaryData.entrySet().stream()
          .map(Map.Entry::getValue)
          .map(Map::keySet)
          .flatMap(Set::stream)
          .distinct()
          .collect(Collectors.toList());
    }

    public void addManaged(Map<T, ?> data) {
      managedData.add(data);
    }

    public void setSyntaxType(String syntaxType) {
      this.syntaxType = syntaxType;
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

  protected final List<T> whichInfo = new ArrayList<>();

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
    SectionNode node = (SectionNode) SkriptLogger.getNode();
    node.convertToEntries(0);

    if (node.getKey().toLowerCase().startsWith("on ")) {
      return false;
    }

    boolean ok = init(args, matchedPattern, parseResult, node);

    SkriptUtil.clearSectionNode(node);

    if (!ok) {
      unregister(null);
    }

    return ok;
  }

  protected abstract boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                                  SectionNode node);

  protected static boolean handleEntriesAndSections(SectionNode node,
                                                    Predicate<EntryNode> entryHandler,
                                                    Predicate<SectionNode> sectionHandler) {
    boolean ok = true;

    for (Node subNode : node) {
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
      } else {
        throw new IllegalStateException();
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
}
