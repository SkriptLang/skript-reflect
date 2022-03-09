package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.ExpressionType;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jdt.annotation.NonNull;

import java.util.Arrays;
import java.util.Optional;

public class ExprPlugin extends SimplePropertyExpression<Object, ObjectWrapper> {

  static {
    Skript.registerExpression(ExprPlugin.class, ObjectWrapper.class, ExpressionType.PROPERTY, "[an] instance of [the] plugin %javatype/string%");
  }

  // PluginManager#getPlugin is case-sensitive, but we don't want scripters to deal with that
  private Optional<String> findPluginCaseInsensitive(String pluginName) {
    return Arrays.stream(Bukkit.getPluginManager().getPlugins())
        .map(Plugin::getName)
        .filter(pluginName::equalsIgnoreCase)
        .findFirst();
  }

  @Override
  public ObjectWrapper convert(Object plugin) {
    if (plugin instanceof String) {
      PluginManager pluginManager = Bukkit.getPluginManager();
      return findPluginCaseInsensitive((String) plugin)
          .map(pluginManager::getPlugin)
          .map(ObjectWrapper::create)
          .orElse(null);
    } else {
      try {
        Class<?> clazz = ((JavaType) plugin).getJavaClass();
        return ObjectWrapper.create(JavaPlugin.getProvidingPlugin(clazz));
      } catch (IllegalArgumentException | IllegalStateException e) {
        return null;
      }
    }
  }

  @Override
  @NonNull
  public Class<? extends ObjectWrapper> getReturnType() {
    return ObjectWrapper.class;
  }

  @Override
  @NonNull
  protected String getPropertyName() {
    return "plugin instance";
  }

}
