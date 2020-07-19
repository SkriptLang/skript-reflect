package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.config.Option;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;

public class SkriptMirror extends JavaPlugin {
  private static SkriptMirror instance;
  private static SkriptAddon addonInstance;

  public SkriptMirror() {
    if (instance == null) {
      instance = this;
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public void onEnable() {
    try {
      getAddonInstance().loadClasses("com.btk5h.skriptmirror.skript");

      Path dataFolder = SkriptMirror.getInstance().getDataFolder().toPath();
      LibraryLoader.loadLibraries(dataFolder);

      ParseOrderWorkarounds.reorderSyntax();

      // Disable *all* and/or warnings
      SkriptReflection.disableAndOrWarnings();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static SkriptAddon getAddonInstance() {
    if (addonInstance == null) {
      addonInstance = Skript.registerAddon(getInstance())
          .setLanguageFileDirectory("lang");
    }
    return addonInstance;
  }

  public static SkriptMirror getInstance() {
    if (instance == null) {
      throw new IllegalStateException();
    }
    return instance;
  }
}
