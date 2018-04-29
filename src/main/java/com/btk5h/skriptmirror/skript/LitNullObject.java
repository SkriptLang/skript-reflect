package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.util.SimpleLiteral;
import com.btk5h.skriptmirror.Null;

public class LitNullObject extends SimpleLiteral<Null> {
  static {
    Skript.registerExpression(LitNullObject.class, Null.class, ExpressionType.SIMPLE, "null");
  }

  public LitNullObject() {
    super(Null.getInstance(), false);
  }
}
