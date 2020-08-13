package com.btk5h.skriptmirror.skript;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
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

@Name("Consent")
@Description({"You can find more information about this here: https://tpgamesnl.gitbook.io/skript-reflect/advanced/experiments"})
public class Consent extends SelfRegisteringSkriptEvent {
  static {
    CustomSyntaxSection.register("Consent", Consent.class, "skript-(mirror|reflect), I know what I'm doing");
  }

  public enum Feature {
    PROXIES("proxies", true),
    DEFERRED_PARSING("deferred-parsing");

    private final String codeName;
    private final boolean outdated;

    Feature(String codeName) {
      this(codeName, false);
    }

    Feature(String codeName, boolean isDeprecated) {
      this.codeName = codeName;
      this.outdated = isDeprecated;
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

    public boolean isOutdated() {
      return outdated;
    }
  }

  private static final Map<File, List<Feature>> consentedFeatures = new HashMap<>();

  private static final String FIRST_CONSENT_LINE =
    "I understand that the following features are experimental and may change in the future.";
  private static final String[] SECOND_CONSENT_LINE = new String[]{
    "I have read about this at https://skript-mirror.gitbook.io/docs/advanced/experiments",
    "I have read about this at https://tpgamesnl.gitbook.io/skript-reflect/advanced/experiments"
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
      assert text != null;

      // For the first few lines, make sure the consent text is copied verbatim
      if ((consentLine == 0 && !text.equals(FIRST_CONSENT_LINE)) ||
        (consentLine == 1 && !(text.equals(SECOND_CONSENT_LINE[0]) || text.equals(SECOND_CONSENT_LINE[1]))))
        return false;

      if (consentLine++ < 2)
        continue;

      Optional<Feature> optionalFeature = Feature.byCodeName(text);
      if (optionalFeature.isPresent()) {
        Feature feature = optionalFeature.get();
        if (feature.isOutdated())
          Skript.warning("This consent feature is no longer needed");

        consentedFeatures
          .computeIfAbsent(currentScript, t -> new ArrayList<>())
          .add(feature);
      } else {
        Skript.error("The feature '" + text + "' doesn't exist or isn't experimental");
        return false;
      }
    }

    SkriptUtil.clearSectionNode(node);
    return true;
  }


  @Override
  public String toString(Event e, boolean debug) {
    return "experimental consent notice";
  }
}
