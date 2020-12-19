package com.btk5h.skriptmirror.skript.reflect.sections;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EffRunSection extends Effect {

  static {
    Skript.registerEffect(EffRunSection.class,
      "run section %section% [(1¦sync|2¦async)] [with [arguments] %-objects%] [and store [the] result in %-objects%] [(4¦and wait)]");
  }

  private Expression<Section> sectionExpression;
  private Kleenean runsAsync;
  private List<Expression<?>> arguments;
  private Expression<?> variableStorage;
  private boolean shouldWait;

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    sectionExpression = SkriptUtil.defendExpression(exprs[0]);

    if ((parseResult.mark & 0b0001) != 0)
      runsAsync = Kleenean.FALSE;
    else if ((parseResult.mark & 0b0010) != 0)
      runsAsync = Kleenean.TRUE;
    else
      runsAsync = Kleenean.UNKNOWN;

    Expression<Object> expr = SkriptUtil.defendExpression(exprs[1]);
    arguments = new ArrayList<>();
    if (expr instanceof ExpressionList) {
      Collections.addAll(arguments, ((ExpressionList<?>) expr).getExpressions());
    } else if (expr != null) {
      arguments.add(expr);
    }

    variableStorage = SkriptUtil.defendExpression(exprs[2]);
    if (variableStorage != null && !(variableStorage instanceof Variable)) {
      Skript.error("The result can only be stored in a variable");
      return false;
    }

    shouldWait = (parseResult.mark & 0b0100) != 0;
    if (!runsAsync.isUnknown() && !shouldWait && variableStorage != null)
      Skript.warning("You need to wait until the section is finished if you want to get a result.");

    if (!runsAsync.isUnknown() && shouldWait)
      ScriptLoader.hasDelayBefore = Kleenean.TRUE;

    return SkriptUtil.canInitSafely(variableStorage) &&
      (arguments.size() == 0 || arguments.stream().allMatch(SkriptUtil::canInitSafely));
  }

  @Override
  protected TriggerItem walk(Event e) {
    if (!runsAsync.isUnknown()) {
      Section section = sectionExpression.getSingle(e);

      Object[][] args = getArgs(e);

      Object localVars = SkriptReflection.removeLocals(e);

      boolean ranAsync = !Bukkit.isPrimaryThread();

      if (section != null) {
        Runnable runnable = () -> {
          SkriptReflection.putLocals(localVars, e);

          section.run(e, args);
          storeResult(section, e);


          if (shouldWait && getNext() != null) {
            Runnable continuation = () -> {
              TriggerItem.walk(getNext(), e);
              SkriptReflection.removeLocals(e);
            };

            if (ranAsync)
              Bukkit.getScheduler().runTaskAsynchronously(SkriptMirror.getInstance(), continuation);
            else
              Bukkit.getScheduler().runTask(SkriptMirror.getInstance(), continuation);
          }
        };

        if (runsAsync.isTrue())
          Bukkit.getScheduler().runTaskAsynchronously(SkriptMirror.getInstance(), runnable);
        else
          Bukkit.getScheduler().runTask(SkriptMirror.getInstance(), runnable);
      } else {
        return getNext();
      }
      return shouldWait ? null : getNext();
    } else {
      return super.walk(e);
    }
  }

  @Override
  protected void execute(Event e) {
    Section section = sectionExpression.getSingle(e);
    if (section != null) {
      section.run(e, getArgs(e));
      storeResult(section, e);
    }
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return "run section " + sectionExpression.toString(e, debug);
  }

  private Object[][] getArgs(Event event) {
    Object[][] args = new Object[arguments.size()][];
    for (int i = 0; i < arguments.size(); i++)
      args[i] = arguments.get(i).getArray(event);
    return args;
  }

  private void storeResult(Section section, Event event) {
    Object[] output = section.getOutput();
    if (variableStorage != null && output != null)
      variableStorage.change(event, output, Changer.ChangeMode.SET);
  }

}
