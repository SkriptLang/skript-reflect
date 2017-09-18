package com.btk5h.skriptmirror;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;

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
      getAddonInstance().loadClasses("com.btk5h.skriptmirror", "skript");
      LibraryLoader.loadLibraries();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static SkriptAddon getAddonInstance() {
    if (addonInstance == null) {
      addonInstance = Skript.registerAddon(getInstance());
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
