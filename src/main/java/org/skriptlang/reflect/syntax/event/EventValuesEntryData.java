package org.skriptlang.reflect.syntax.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.registrations.Classes;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;

import java.util.ArrayList;
import java.util.List;

public class EventValuesEntryData extends KeyValueEntryData<List<ClassInfo<?>>> {

	private static final String listSplitPattern = "\\s*,?\\s+(and|n?or)\\s+|\\s*,\\s*"; // Found in SkriptParser

	public EventValuesEntryData(String key, @Nullable List<ClassInfo<?>> defaultValue, boolean optional) {
		super(key, defaultValue, optional);
	}

	@Override
	@Nullable
	protected List<ClassInfo<?>> getValue(String value) {
		String[] stringClasses = value.split(listSplitPattern);
		List<ClassInfo<?>> classInfos = new ArrayList<>(stringClasses.length);
		for (String stringClass : stringClasses) {
			ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(stringClass);
			if (classInfo == null) {
				Skript.error("The type " + stringClass + " doesn't exist");
				return null;
			}
			classInfos.add(classInfo);
		}
		return classInfos;
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
