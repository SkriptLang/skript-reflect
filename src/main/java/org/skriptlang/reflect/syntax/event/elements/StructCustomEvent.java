package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;

import java.util.*;

public class StructCustomEvent extends CustomSyntaxStructure<EventSyntaxInfo> {

  public static boolean customEventsUsed = false;

  static {
    String[] syntax = {
      "[(1¦local)] [custom] event %string%"
    };
    Skript.registerStructure(StructCustomEvent.class, customSyntaxValidator()
        .addEntry("pattern", null, true)
        .addEntry("event-values", null, true)
        .addSection("check", true)
        .build(), syntax);
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

  private SectionNode parseNode, checkNode;

  @Override
  protected DataTracker<EventSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         EntryContainer entryContainer) {
    customEventsUsed = true;

    Script script = (parseResult.mark & 1) == 1 ? SkriptUtil.getCurrentScript() : null;

    List<String> patternStrings = new ArrayList<>();

    SectionNode patternsNode = entryContainer.getOptional("patterns", SectionNode.class, false);
    String patternNode = entryContainer.getOptional("pattern", String.class, false);
    if (patternsNode != null) {
      for (Node subNode : patternsNode) {
        patternStrings.add(subNode.getKey());
      }
    } else if (patternNode != null) {
      patternStrings.add(patternNode);
    } else {
      Skript.error("You need at least one pattern");
      return false;
    }

    int i = 1;
    for (String pattern : patternStrings) {
      register(EventSyntaxInfo.create(script, pattern, i++));
    }

    if (whichInfo.isEmpty()) {
      Skript.error("You need at least one pattern");
      return false;
    }

    String name = (String) args[0].getSingle();
    if (nameValues.values().stream().anyMatch(name::equalsIgnoreCase)) {
      Skript.error("There is already a custom event with that name");
      return false;
    }

    whichInfo.forEach(which -> nameValues.put(which, name));

    return true;
  }

  @Override
  public boolean preLoad() {
    super.preLoad();

    EntryContainer entryContainer = getEntryContainer();
    parseNode = entryContainer.getOptional("parse", SectionNode.class, false);
    SectionNode safeParseNode = entryContainer.getOptional("safe parse", SectionNode.class, false);
    if (parseNode != null) {
      if (safeParseNode != null) {
        Skript.error("You can't have two parse sections");
        return false;
      }
      whichInfo.forEach(which -> parseSectionLoaded.put(which, false));
    } else if (safeParseNode != null) {
      SyntaxParseEvent.register(safeParseNode, whichInfo, parserHandlers);
      whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
    }

    return true;
  }

  @Override
  public boolean load() {
    EntryContainer entryContainer = getEntryContainer();

    String[] stringClasses = Optional.<String>ofNullable(entryContainer.getOptional("event-values", String.class, false))
        .map(eventValues -> eventValues.split(listSplitPattern))
        .orElse(null);
    if (stringClasses != null) {
      List<ClassInfo<?>> classInfoList = new ArrayList<>(stringClasses.length);
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
    }

    if (parseNode != null) {
      SkriptLogger.setNode(parseNode);
      SyntaxParseEvent.register(parseNode, whichInfo, parserHandlers);

      whichInfo.forEach(which -> parseSectionLoaded.put(which, true));
    }

    checkNode = entryContainer.getOptional("check", SectionNode.class, false);
    if (checkNode != null) {
      SkriptLogger.setNode(checkNode);

      getParser().setCurrentEvent("custom event trigger", EventTriggerEvent.class);
      CustomEvent.setLastWhich(whichInfo.get(0));
      List<TriggerItem> items = SkriptUtil.getItemsFromNode(checkNode);
      CustomEvent.setLastWhich(null);
      whichInfo.forEach(which ->
          eventHandlers.put(which,
              new Trigger(getParser().getCurrentScript(), "event " + which, new SimpleEvent(), items)));
    }
    SkriptLogger.setNode(null);

    return true;
  }

  public static EventSyntaxInfo lookup(Script script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }

}
