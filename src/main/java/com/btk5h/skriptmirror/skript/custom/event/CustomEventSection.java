package com.btk5h.skriptmirror.skript.custom.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CustomEventSection extends CustomSyntaxSection<EventSyntaxInfo> {

  static {
    CustomSyntaxSection.register("Define Event", CustomEventSection.class,
      "[(1¦local)] event <.+>",
      "[(1¦local)] event");
  }

  private static final DataTracker<EventSyntaxInfo> dataTracker = new DataTracker<>();

  static Map<EventSyntaxInfo, Trigger> triggers = new HashMap<>();
  static Map<EventSyntaxInfo, String> eventValues = new HashMap<>();

  static {
    dataTracker.setSyntaxType("event");

    Skript.registerEvent("custom event", CustomEvent.class, BukkitCustomEvent.class);
    Optional<SkriptEventInfo<?>> info = Skript.getEvents().stream()
      .filter(i -> i.c == CustomEvent.class)
      .findFirst();
    info.ifPresent(dataTracker::setInfo);

    dataTracker.addManaged(triggers);
    dataTracker.addManaged(eventValues);
  }

  @Override
  protected DataTracker<EventSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

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

    return handleEntriesAndSections(node,
      entryNode -> {
        //noinspection ConstantConditions
        if (entryNode.getKey().equalsIgnoreCase("event-values")) {
          String eventValue = entryNode.getValue();
          whichInfo.forEach(which -> eventValues.put(which, eventValue));
          return true;
        }
        return false;
      },
      sectionNode -> {
        String key = sectionNode.getKey();

        if (key.equalsIgnoreCase("patterns")) {
          return true;
        }

        if (key.equalsIgnoreCase("trigger")) {
          ScriptLoader.setCurrentEvent("custom event trigger", EventTriggerEvent.class);
          List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
          whichInfo.forEach(which ->
            triggers.put(which,
              new Trigger(SkriptUtil.getCurrentScript(), "event " + which, this, items)));
          return true;
        }

        return false;
      }
    );
  }

}
