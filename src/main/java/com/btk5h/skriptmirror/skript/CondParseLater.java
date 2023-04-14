package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Statement;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.ParserInstanceState;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

public class CondParseLater extends Condition {
  public static boolean deferredParsingUsed = false;

  static {
    Skript.registerCondition(CondParseLater.class, "\\(parse[d] later\\) <.+>");
  }

  private String statement;
  private ParserInstanceState parserInstanceState;
  private Statement parsedStatement;

  @Override
  public boolean check(Event e) {
    Condition parsedCondition = getParsedCondition();

    if (parsedCondition == null)
      return false;

    return parsedCondition.check(e);
  }

  @Override
  protected TriggerItem walk(Event e) {
    Statement parsedStatement = getParsedStatement();

    return parsedStatement == null ? getNext() : parsedStatement;
  }

  @Override
  public String toString(Event e, boolean debug) {
    if (parsedStatement != null) {
      return "parsed later: " + parsedStatement.toString(e, debug);
    }

    return "not parsed yet: " + statement;
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    deferredParsingUsed = true;

    if (!Consent.Feature.DEFERRED_PARSING.hasConsent(SkriptUtil.getCurrentScriptFile())) {
      Skript.error("This feature requires consent, because it is experimental.");
      return false;
    }

    statement = parseResult.regexes.get(0).group();
    parserInstanceState = ParserInstanceState.copyOfCurrentState();
    return true;
  }

  private Statement getParsedStatement() {
    if (parsedStatement == null) {
      ParserInstanceState previousState = ParserInstanceState.copyOfCurrentState();
      parserInstanceState.applyToCurrentState();
      parsedStatement = Statement.parse(statement, "Could not parse condition/effect at runtime: "
        + statement);
      previousState.applyToCurrentState();

      if (parsedStatement != null) {
        parsedStatement.setNext(getNext());
        parsedStatement.setParent(getParent());
      }
    }

    return parsedStatement;
  }

  private Condition getParsedCondition() {
    if (parsedStatement == null) {
      parserInstanceState.applyToCurrentState();
      parsedStatement = Condition.parse(statement, "Could not parse condition at runtime: " + statement);

      if (parsedStatement != null) {
        parsedStatement.setNext(getNext());
        parsedStatement.setParent(getParent());
      }
    }

    if (parsedStatement != null && !(parsedStatement instanceof Condition)) {
      throw new IllegalStateException(statement + " was used as a condition but was parsed as a statement");
    }

    return (Condition) parsedStatement;
  }
}
