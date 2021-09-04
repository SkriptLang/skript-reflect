package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.skript.custom.PreloadListener;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CustomEventSection extends CustomSyntaxSection<EventSyntaxInfo> {
  public static boolean customEventsUsed = false;

  static {
    String[] syntax = {
      "[(1¦local)] [custom] event %string%"
    };
    CustomSyntaxSection.register("Define Event", CustomEventSection.class, syntax);
    PreloadListener.addSyntax(CustomEventSection.class, syntax);
  }

  private static final DataTracker<EventSyntaxInfo> dataTracker = new DataTracker<>();
  private static final String listSplitPattern = "\\s*,?\\s+(and|n?or)\\s+|\\s*,\\s*"; // Found in SkriptParser

  static final Map<EventSyntaxInfo, String> nameValues = new HashMap<>();
  static final Map<EventSyntaxInfo, List<ClassInfo<?>>> eventValueTypes = new HashMap<>();
  static final Map<EventSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
  static final Map<EventSyntaxInfo, Trigger> eventHandlers = new HashMap<>();
  static final Map<EventSyntaxInfo, Boolean> parseSectionLoaded = new HashMap<>();

  static {
    Skript.registerEvent("custom event", CustomEvent.class, BukkitCustomEvent.class);
    Optional<SkriptEventInfo<?>> info = Skript.getEvents().stream()
      .filter(i -> i.c == CustomEvent.class)
      .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(nameValues);
    dataTracker.addManaged(eventValueTypes);
    dataTracker.addManaged(parserHandlers);
    dataTracker.addManaged(eventHandlers);
    dataTracker.addManaged(parseSectionLoaded);
  }

  @Override
  protected DataTracker<EventSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         SectionNode node, boolean isPreload) {
    customEventsUsed = true;

    if (!isPreloaded) {
      File script = (parseResult.mark & 1) == 1 ? SkriptUtil.getCurrentScript() : null;

      List<String> patternStrings = new ArrayList<>();

      SectionNode patternsNode = (SectionNode) node.get("patterns");
      EntryNode patternNode = (EntryNode) node.get("pattern");
      if (patternsNode != null) {
        for (Node subNode : patternsNode) {
          patternStrings.add(subNode.getKey());
        }
      } else if (patternNode != null) {
        patternStrings.add(patternNode.getValue());
      } else {
        Skript.error("You need at least one pattern");
        return false;
      }

      int i = 1;
      for (String pattern : patternStrings) {
        register(EventSyntaxInfo.create(script, pattern, i++));
      }

      if (whichInfo.size() == 0) {
        Skript.error("You need at least one pattern");
        return false;
      }

      String name = (String) args[0].getSingle();
      if (nameValues.values().stream().anyMatch(name::equalsIgnoreCase)) {
        Skript.error("There is already a custom event with that name");
        return false;
      }

      whichInfo.forEach(which -> nameValues.put(which, name));

      if (node.get("parse") != null) {
        if (node.get("safe parse") != null) {
          Skript.error("You can't have two parse sections");
          return false;
        }
        whichInfo.forEach(which -> parseSectionLoaded.put(which, false));
      } else {
        SectionNode safeParseNode = (SectionNode) node.get("safe parse");
        if (safeParseNode != null) {
          SyntaxParseEvent.register(this, safeParseNode, whichInfo, parserHandlers);

          whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
        }
      }
    }

    boolean ok = handleEntriesAndSections(node,
      entryNode -> {
        String key = entryNode.getKey();
        assert key != null;

        if (key.equalsIgnoreCase("pattern"))
          return true;

        if (key.equalsIgnoreCase("event-values")) {
          String[] stringClasses = entryNode.getValue().split(listSplitPattern);
          List<ClassInfo<?>> classInfoList = new ArrayList<>();
          for (String stringClass : stringClasses) {
            ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(stringClass);
            if (classInfo == null) {
              Skript.error("The type " + stringClass + " doesn't exist");
              return false;
            }
            classInfoList.add(classInfo);
          }

          SkriptReflection.replaceEventValues(classInfoList);
          whichInfo.forEach(which -> eventValueTypes.put(which, classInfoList));
          return true;
        }

        return false;
      },
      sectionNode -> {
        String key = sectionNode.getKey();
        assert key != null;

        if (key.equalsIgnoreCase("patterns"))
          return true;

        if (key.equalsIgnoreCase("parse"))
          return true;

        if (key.equalsIgnoreCase("safe parse"))
          return true;

        if (key.equalsIgnoreCase("check"))
          return true;

        return false;
      }
    );

    if (!isPreload) {
      SectionNode sectionNode = (SectionNode) node.get("parse");
      if (sectionNode != null) {
        SkriptLogger.setNode(sectionNode);
        SyntaxParseEvent.register(this, sectionNode, whichInfo, parserHandlers);

        whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
      }

      sectionNode = (SectionNode) node.get("check");
      if (sectionNode != null) {
        SkriptLogger.setNode(sectionNode);

        getParser().setCurrentEvent("custom event trigger", EventTriggerEvent.class);
        CustomEvent.setLastWhich(whichInfo.get(0));
        List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
        CustomEvent.setLastWhich(null);
        whichInfo.forEach(which ->
          eventHandlers.put(which,
            new Trigger(SkriptUtil.getCurrentScript(), "event " + which, this, items)));
      }
      SkriptLogger.setNode(null);
    }

    return ok;
  }

  public static EventSyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }

}
