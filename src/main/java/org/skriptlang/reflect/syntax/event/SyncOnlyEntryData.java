package org.skriptlang.reflect.syntax.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SimpleNode;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;

public class SyncOnlyEntryData extends KeyValueEntryData<Boolean> {

  public SyncOnlyEntryData(String key, @Nullable Boolean defaultValue, boolean optional) {
    super(key, defaultValue, optional);
  }

  @Override
  @Nullable
  protected Boolean getValue(String value) {
    if (!(value.equalsIgnoreCase("false") || value.equalsIgnoreCase("true"))) {
        Skript.error("Sync only entry can be only true or false.");
        return null;
    }
    return Boolean.valueOf(value);
  }

  @Override
  public final boolean canCreateWith(Node node) {
    if (!(node instanceof SimpleNode))
      return false;
    String key = node.getKey();
    if (key == null)
      return false;
    return canCreateWith(ScriptLoader.replaceOptions(key));
  }

  protected boolean canCreateWith(String node) {
    return node.startsWith(getKey() + getSeparator());
  }

}
