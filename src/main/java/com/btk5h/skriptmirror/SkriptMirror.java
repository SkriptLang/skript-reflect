package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Version;
import org.skriptlang.reflect.syntax.condition.elements.StructCustomCondition;
import org.skriptlang.reflect.syntax.effect.elements.StructCustomEffect;
import org.skriptlang.reflect.syntax.event.elements.StructCustomEvent;
import org.skriptlang.reflect.syntax.expression.elements.StructCustomExpression;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import com.btk5h.skriptmirror.skript.reflect.ExprProxy;
import com.btk5h.skriptmirror.skript.reflect.sections.SecSection;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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
		if (!Bukkit.getPluginManager().isPluginEnabled("Skript")) {
			getLogger().severe("Disabling skript-reflect because Skript is disabled");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		if (Skript.getVersion().isSmallerThan(new Version("2.14.0-pre1"))) {
			getLogger().severe("");
			getLogger().severe("Your version of Skript (" + Skript.getVersion() + ") is not supported, at least Skript 2.14 is required to run this version of skript-reflect.");
			getLogger().severe("");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		if (Bukkit.getPluginManager().getPlugin("skript-mirror") != null) {
			getLogger().warning("You shouldn't have both skript-mirror and skript-reflect enabled, it will probably cause issues");
		}

		try {
			getAddonInstance()
				.loadClasses("com.btk5h.skriptmirror.skript")
				.loadClasses("org.skriptlang.reflect", "syntax", "java.elements");

			Path dataFolder = SkriptMirror.getInstance().getDataFolder().toPath();
			LibraryLoader.loadLibraries(dataFolder);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Comparators.registerComparator(ClassInfo.class, JavaType.class, (classInfo, javaType) -> {
			ClassInfo<?> matchingClassInfo = Classes.getExactClassInfo(javaType.getJavaClass());
			if (matchingClassInfo == null)
				return Relation.NOT_EQUAL;
			return Comparators.compare(classInfo, matchingClassInfo);
		});

		ParseOrderWorkarounds.reorderSyntax();

		// Disable *all* and/or warnings
		SkriptReflection.disableAndOrWarnings();

		Metrics metrics = new Metrics(this, 10157);

		metrics.addCustomChart(new Metrics.DrilldownPie("skript_version", () -> {
			Map<String, Map<String, Integer>> map = new HashMap<>();

			Version version = Skript.getVersion();
			Map<String, Integer> entry = new HashMap<>();
			entry.put(version.toString(), 1);

			map.put("" + version.getMajor() + "." + version.getMinor(), entry);

			return map;
		}));

		metrics.addCustomChart(new Metrics.SingleLineChart("java_calls_made", () -> {
			int i = ExprJavaCall.javaCallsMade;
			ExprJavaCall.javaCallsMade = 0;
			return i;
		}));

		metrics.addCustomChart(new Metrics.SimplePie("custom_conditions_used",
			() -> "" + StructCustomCondition.customConditionsUsed));
		metrics.addCustomChart(new Metrics.SimplePie("custom_effects_used",
			() -> "" + StructCustomEffect.customEffectsUsed));
		metrics.addCustomChart(new Metrics.SimplePie("custom_events_used",
			() -> "" + StructCustomEvent.customEventsUsed));
		metrics.addCustomChart(new Metrics.SimplePie("custom_expressions_used",
			() -> "" + StructCustomExpression.customExpressionsUsed));

		metrics.addCustomChart(new Metrics.SimplePie("proxies_used",
			() -> "" + ExprProxy.proxiesUsed));
		metrics.addCustomChart(new Metrics.SimplePie("sections_used",
			() -> "" + SecSection.sectionsUsed));

	}

	public static SkriptAddon getAddonInstance() {
		if (addonInstance == null) {
			addonInstance = Skript.registerAddon(getInstance()).setLanguageFileDirectory("lang");
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
