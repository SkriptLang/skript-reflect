package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public abstract class CustomSyntaxSection<T extends CustomSyntaxSection.SyntaxData> extends SelfRegisteringSkriptEvent {
  @SuppressWarnings("unchecked")
  public static <E extends SkriptEvent> SkriptEventInfo<E> register(String name, Class<E> c, String... patterns) {
    return Skript.registerEvent("*" + name, c, new Class[0], patterns);
  }

  public static class DataTracker<T> {
    public DataTracker() {
    }

    private final SectionValidator validator = new SectionValidator();
    private List<String> patterns = new ArrayList<>();
    private final Map<File, Map<String, T>> primaryData = new HashMap<>();
    private final List<Map<T, ?>> managedData = new ArrayList<>();
    private String syntaxType = "syntax";
    private SyntaxElementInfo<?> info;

    public SectionValidator getValidator() {
      return validator;
    }

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

    protected SyntaxData(File script, String pattern) {
      this.script = script;
      this.pattern = pattern;
    }

    public File getScript() {
      return script;
    }

    public String getPattern() {
      return pattern;
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

    if (node.getKey().toLowerCase().startsWith("on ") || !getDataTracker().getValidator().validate(node)) {
      return false;
    }

    boolean ok = init(args, matchedPattern, parseResult, node);

    if (!ok) {
      unregister(null);
    }

    SkriptUtil.clearSectionNode(node);

    return ok;
  }

  protected abstract boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                                  SectionNode node);

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
