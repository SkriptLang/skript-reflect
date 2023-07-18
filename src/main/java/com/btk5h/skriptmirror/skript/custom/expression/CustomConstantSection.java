package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.StringUtils;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

public class CustomConstantSection extends CustomSyntaxSection<ConstantSyntaxInfo> {

  static {
    CustomSyntaxSection.register("Define Constant", CustomConstantSection.class,
        "option <.+>");
  }

  private static final DataTracker<ConstantSyntaxInfo> dataTracker = new DataTracker<>();

  static {
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

  @SuppressWarnings({"SwitchStatementWithTooFewBranches"})
  @Override
  protected boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                         SectionNode node, boolean isPreload) {
    String what;

    switch (matchedPattern) {
      case 0:
        what = parseResult.regexes.get(0).group();

        AtomicBoolean hasGetSection = new AtomicBoolean();
        boolean nodesOkay = handleEntriesAndSections(node,
          entryNode -> false,
          sectionNode -> {
            String key = sectionNode.getKey();
            assert key != null;

            if (key.equalsIgnoreCase("get")) {
              getParser().setCurrentEvent("custom constant getter", ConstantGetEvent.class);
              List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
              Trigger getter =
                  new Trigger(getParser().getCurrentScript(), "get {@" + what + "}", this, items);

              computeOption(what, getter);

              hasGetSection.set(true);
              return true;
            }

            return false;
          });

        if (!hasGetSection.get())
          Skript.warning("Computed options don't work without a get section");

        return nodesOkay;
    }

    return false;
  }

  private static void computeOption(String option, Trigger getter) {
    ConstantGetEvent constantEvent = new ConstantGetEvent(0, null);
    getter.execute(constantEvent);
    ParserInstance.get().getCurrentOptions().put(option, StringUtils.join(constantEvent.getOutput()));
  }

}

