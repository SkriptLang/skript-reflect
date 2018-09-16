package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.util.StringUtils;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class CustomConstantSection extends CustomSyntaxSection<ConstantSyntaxInfo> {
  static {
    //noinspection unchecked
    CustomSyntaxSection.register("Define Constant", CustomConstantSection.class,
        "option <.+>");
    // TODO add support for custom constant expressions
  }

  private static DataTracker<ConstantSyntaxInfo> dataTracker = new DataTracker<>();

  static {
    dataTracker.setSyntaxType("constant");

    // noinspection unchecked
    Skript.registerExpression(CustomExpression.class, Object.class, ExpressionType.SIMPLE);
    Optional<ExpressionInfo<?, ?>> info = StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(Skript.getExpressions(), Spliterator.ORDERED), false)
        .filter(i -> i.c == CustomExpression.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);
  }

  @Override
  protected DataTracker<ConstantSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean init(Literal[] args, int matchedPattern, SkriptParser.ParseResult parseResult, SectionNode node) {
    String what;


    switch (matchedPattern) {
      case 0:
        what = parseResult.regexes.get(0).group();
        return handleEntriesAndSections(node,
            entryNode -> false,
            sectionNode -> {
              String key = sectionNode.getKey();

              if (key.equalsIgnoreCase("get")) {
                ScriptLoader.setCurrentEvent("custom constant getter", ConstantGetEvent.class);
                List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
                Trigger getter =
                    new Trigger(SkriptUtil.getCurrentScript(), "get {@" + what + "}", this, items);
                
                computeOption(what, getter);

                return true;
              }

              return false;
            });
    }

    return false;
  }

  private static void computeOption(String option, Trigger getter) {
    ConstantGetEvent constantEvent = new ConstantGetEvent(0, null);
    getter.execute(constantEvent);
    SkriptReflection.getCurrentOptions().put(option, StringUtils.join(constantEvent.getOutput()));
  }
}

