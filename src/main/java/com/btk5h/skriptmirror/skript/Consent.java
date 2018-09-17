package com.btk5h.skriptmirror.skript;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SelfRegisteringSkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.io.File;
import java.util.*;

public class Consent extends SelfRegisteringSkriptEvent {
  static {
    CustomSyntaxSection.register("Consent", Consent.class, "skript-mirror, I know what I'm doing");
  }

  public enum Feature {
    DEFERRED_PARSING("deferred-parsing");

    private String codeName;

    Feature(String codeName) {
      this.codeName = codeName;
    }

    public boolean hasConsent(File script) {
      List<Feature> features = consentedFeatures.get(script);
      return features != null && features.contains(this);
    }

    public static Optional<Feature> byCodeName(String codeName) {
      return Arrays.stream(Feature.values())
          .filter(f -> codeName.equals(f.codeName))
          .findFirst();
    }
  }

  private static final Map<File, List<Feature>> consentedFeatures = new HashMap<>();

  private static final String[] CONSENT_LINES = new String[]{
      "I understand that the following features are experimental and may change in the future.",
      "I have read about this at https://skript-mirror.gitbook.io/docs/advanced/experiments"
  };

  @Override
  public void register(Trigger t) {
  }

  @Override
  public void unregister(Trigger t) {
    consentedFeatures.remove(t.getScript());
  }

  @Override
  public void unregisterAll() {
    consentedFeatures.clear();
  }

  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
    File currentScript = ScriptLoader.currentScript.getFile();
    SectionNode node = ((SectionNode) SkriptLogger.getNode());

    if (node.getKey().toLowerCase().startsWith("on ")) {
      return false;
    }

    int consentLine = 0;
    for (Node subNode : node) {
      String text = subNode.getKey();

      // For the first few lines, make sure the consent text is copied verbatim
      if (consentLine < CONSENT_LINES.length) {
        if (!text.equals(CONSENT_LINES[consentLine])) {
          return false;
        }

        consentLine++;
        continue;
      }

      Feature.byCodeName(text).ifPresent(feature -> {
        consentedFeatures
            .computeIfAbsent(currentScript, t -> new ArrayList<>())
            .add(feature);
      });
    }

    SkriptUtil.clearSectionNode(node);
    return true;
  }


  @Override
  public String toString(Event e, boolean debug) {
    return "import";
  }
}
