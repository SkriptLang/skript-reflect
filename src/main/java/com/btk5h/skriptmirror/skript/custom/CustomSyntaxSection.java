package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.Util;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CustomSyntaxSection<T extends CustomSyntaxSection.SyntaxData> extends SelfRegisteringSkriptEvent {
  public static class DataTracker<T> {
    public DataTracker() {
    }

    private final SectionValidator validator = new SectionValidator();
    private final List<String> patterns = new ArrayList<>();
    private final Map<String, T> primaryData = new HashMap<>();
    private final List<Map<T, ?>> managedData = new ArrayList<>();
    private String syntaxType = "syntax";
    private SyntaxElementInfo<?> info;

    public SectionValidator getValidator() {
      return validator;
    }

    public List<String> getPatterns() {
      return patterns;
    }

    public Map<String, T> getPrimaryData() {
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

    public void addManaged(Map<T, ?> data) {
      managedData.add(data);
    }

    public void setSyntaxType(String syntaxType) {
      this.syntaxType = syntaxType;
    }

    public void setInfo(SyntaxElementInfo<?> info) {
      this.info = info;
    }


    public final T lookup(int matchedPattern) {
      String originalSyntax = patterns.get(matchedPattern);
      return primaryData.get(originalSyntax);
    }
  }

  public interface SyntaxData {
    String getPattern();
  }

  protected final List<T> whichInfo = new ArrayList<>();

  protected abstract DataTracker<T> getDataTracker();

  @Override
  public void register(Trigger t) {
  }

  @Override
  public void unregister(Trigger t) {
    whichInfo.forEach(which -> {
      getDataTracker().getPatterns().remove(which.getPattern());
      getDataTracker().getPrimaryData().remove(which.getPattern());
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

    if (!getDataTracker().getValidator().validate(node)) {
      return false;
    }

    boolean ok = init(args, matchedPattern, parseResult, node);

    if (!ok) {
      unregister(null);
    }

    Util.clearSectionNode(node);
    update();

    return ok;
  }

  protected abstract boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                                  SectionNode node);

  @Override
  public String toString(Event e, boolean debug) {
    return null;
  }

  private void update() {
    Util.setPatterns(getDataTracker().getInfo(), getDataTracker().getPatterns().toArray(new String[0]));
  }

  protected final void register(T data) {
    String pattern = data.getPattern();

    whichInfo.add(data);

    getDataTracker().getPatterns().add(pattern);
    getDataTracker().getPrimaryData().put(pattern, data);
  }
}
