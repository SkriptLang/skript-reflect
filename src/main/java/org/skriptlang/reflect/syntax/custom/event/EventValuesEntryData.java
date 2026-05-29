package org.skriptlang.reflect.syntax.custom.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.Utils.PluralResult;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;

import java.util.ArrayList;
import java.util.List;

public class EventValuesEntryData extends KeyValueEntryData<List<Class<?>>> {

	public EventValuesEntryData(String key, @Nullable List<Class<?>> defaultValue, boolean optional) {
		super(key, defaultValue, optional);
	}

	@Override
	@Nullable
	protected List<Class<?>> getValue(String value) {
		String[] stringClasses = SkriptParser.LIST_SPLIT_PATTERN.split(value);
		List<Class<?>> infos = new ArrayList<>(stringClasses.length);
		for (String stringClass : stringClasses) {
			PluralResult result = Utils.isPlural(stringClass);
			String input = result.updated();
			boolean plural = result.plural();
			ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(input);
			if (classInfo == null) {
				Skript.error("The type " + stringClass + " doesn't exist");
				return null;
			}
			Class<?> type = classInfo.getC();
			infos.add(plural ? type.arrayType() : type);
		}
		return infos;
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

	protected boolean canCreateWith(String key) {
		return key.startsWith(getKey() + getSeparator());
	}

}
