package com.btk5h.skriptmirror.skript.custom.expression;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.util.StringUtils;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxSection;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;

import java.io.File;
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

    dataTracker.getValidator()
        .addSection("get", false);

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
    SectionNode patterns = (SectionNode) node.get("patterns");
    File script = (parseResult.mark & 1) == 1 ? SkriptUtil.getCurrentScript() : null;


    ScriptLoader.setCurrentEvent("custom constant getter", ConstantGetEvent.class);
    Optional<List<TriggerItem>> getterItems = SkriptUtil.getItemsFromNode(node, "get");

    switch (matchedPattern) {
      case 0:
        what = parseResult.regexes.get(0).group();
        if (getterItems.isPresent()) {
          Trigger getter =
              new Trigger(ScriptLoader.currentScript.getFile(), "get {@" + what + "}", this, getterItems.get());
          computeOption(what, getter);
        }
        return true;
      case 1:
        what = parseResult.regexes.get(0).group();
        register(ConstantSyntaxInfo.create(script, what, 1));
        break;
      case 2:
        if (patterns == null) {
          Skript.error("Custom expressions without inline patterns must have a patterns section.");
          return false;
        }

        int i = 1;
        for (Node subNode : patterns) {
          register(ConstantSyntaxInfo.create(script, subNode.getKey(), i++));
        }
        break;
    }

    if (matchedPattern != 1 && patterns != null) {
      Skript.error("Custom expressions with inline patterns may not have a patterns section.");
      return false;
    }

    return true;
  }

  private static void computeOption(String option, Trigger getter) {
    ConstantGetEvent constantEvent = new ConstantGetEvent(0, null);
    getter.execute(constantEvent);
    SkriptReflection.getCurrentOptions().put(option, StringUtils.join(constantEvent.getOutput()));
  }
}

