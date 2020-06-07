package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.*;
import ch.njol.skript.registrations.Classes;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.Bukkit;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CustomEventSection extends CustomSyntaxSection<EventSyntaxInfo> {

  static {
    CustomSyntaxSection.register("Define Event", CustomEventSection.class,
      "[(1¦local)] [custom] event <.+>",
      "[(1¦local)] [custom] event");
  }

  private static final DataTracker<EventSyntaxInfo> dataTracker = new DataTracker<>();
  private static final String listSplitPattern = "\\s*,?\\s+(and|n?or)\\s+|\\s*,\\s*"; // Found in SkriptParser

  static Map<EventSyntaxInfo, String> nameValues = new HashMap<>();
  static Map<EventSyntaxInfo, List<ClassInfo<?>>> eventValueTypes = new HashMap<>();
  static Map<EventSyntaxInfo, Trigger> parserHandlers = new HashMap<>();
  static Map<EventSyntaxInfo, Trigger> eventHandlers = new HashMap<>();

  static {
    dataTracker.setSyntaxType("event");

    Skript.registerEvent("custom event", CustomEvent.class, BukkitCustomEvent.class);
    Optional<SkriptEventInfo<?>> info = Skript.getEvents().stream()
      .filter(i -> i.c == CustomEvent.class)
      .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(nameValues);
    dataTracker.addManaged(eventValueTypes);
    dataTracker.addManaged(parserHandlers);
    dataTracker.addManaged(eventHandlers);
  }

  @Override
  protected DataTracker<EventSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, SectionNode node) {
    SectionNode patterns = (SectionNode) node.get("patterns");
    File script = (parseResult.mark & 1) == 1 ? SkriptUtil.getCurrentScript() : null;

    switch (matchedPattern) {
      case 0:
        register(EventSyntaxInfo.create(script, parseResult.regexes.get(0).group(), 1));
        break;
      case 1:
        if (patterns == null) {
          Skript.error("Custom events without inline patterns must have a patterns section.");
          return false;
        }

        int i = 1;
        for (Node subNode : patterns) {
          register(EventSyntaxInfo.create(script, subNode.getKey(), i++));
        }
        break;
    }

    if (matchedPattern != 1 && patterns != null) {
      Skript.error("Custom events with inline patterns may not have a patterns section.");
      return false;
    }

    if (whichInfo.size() == 0) {
      Skript.error("You need at least one pattern");
      return false;
    }

    AtomicBoolean hasName = new AtomicBoolean();
    boolean nodesOkay = handleEntriesAndSections(node,
      entryNode -> {
        String key = entryNode.getKey();
        assert key != null;

        if (key.equalsIgnoreCase("name")) {
          String nameValue = entryNode.getValue();
          whichInfo.forEach(which -> nameValues.put(which, nameValue));
          hasName.set(true);
          return true;
        }

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

          replaceEventValues(classInfoList);
          whichInfo.forEach(which -> eventValueTypes.put(which, classInfoList));
          return true;
        }

        return false;
      },
      sectionNode -> {
        String key = sectionNode.getKey();
        assert key != null;

        if (key.equalsIgnoreCase("patterns")) {
          return true;
        }

        if (key.equalsIgnoreCase("parse")) {
          SyntaxParseEvent.register(this, sectionNode, whichInfo, parserHandlers);
          return true;
        }

        if (key.equalsIgnoreCase("check")) {
          ScriptLoader.setCurrentEvent("custom event trigger", EventTriggerEvent.class);
          List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
          whichInfo.forEach(which ->
            eventHandlers.put(which,
              new Trigger(SkriptUtil.getCurrentScript(), "event " + which, this, items)));
          return true;
        }

        return false;
      }
    );

    if (!nodesOkay)
      return false;

    if (!hasName.get()) {
      Skript.error("The custom event needs a name");
      return false;
    }

    String name = nameValues.get(whichInfo.get(0));
    if (nameValues.values().stream().filter(nameValue -> nameValue.equalsIgnoreCase(name)).count() != 1) {
      Skript.error("There is already a custom event with that name");
      return false;
    }

    return true;
  }

  private static Field defaultExpressionField;

  static {
    try {
      defaultExpressionField = ClassInfo.class.getDeclaredField("defaultExpression");
      defaultExpressionField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  private void replaceEventValues(List<ClassInfo<?>> classInfoList) {
    if (defaultExpressionField == null)
      return;

    try {
      for (ClassInfo<?> classInfo : classInfoList) {
        DefaultExpression<?> defaultExpression = classInfo.getDefaultExpression();
        if (defaultExpression instanceof EventValueExpression && !(defaultExpression instanceof ExprReplacedEventValue)) {
          ExprReplacedEventValue<?> replacedEventValue =
            new ExprReplacedEventValue<>((EventValueExpression<?>) defaultExpression);
          defaultExpressionField.set(classInfo, replacedEventValue);
          replaceExtra(classInfo);
        }
      }
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  private void replaceExtra(ClassInfo<?> classInfo) {
    List<ClassInfo<?>> allClassInfos = Classes.getClassInfos();
    List<ClassInfo<?>> classInfoList = allClassInfos.stream()
      .filter(loopedClassInfo -> loopedClassInfo != classInfo)
      .filter(loopedClassInfo -> classInfo.getC().isAssignableFrom(loopedClassInfo.getC())
        || loopedClassInfo.getC().isAssignableFrom(classInfo.getC()))
      .collect(Collectors.toList());
    replaceEventValues(classInfoList);
  }

  public static EventSyntaxInfo lookup(File script, int matchedPattern) {
    return dataTracker.lookup(script, matchedPattern);
  }

}
