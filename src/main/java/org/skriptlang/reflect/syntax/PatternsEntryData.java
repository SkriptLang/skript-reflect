package org.skriptlang.reflect.syntax;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;

import java.util.ArrayList;
import java.util.List;

class PatternsEntryData extends EntryData<List<String>> {

	public PatternsEntryData(String key, @Nullable List<String> defaultValue, boolean optional) {
		super(key, defaultValue, optional);
	}

	@Override
	public List<String> getValue(Node node) {
		List<String> patterns = new ArrayList<>();
		for (Node subNode : (SectionNode) node) {
			String key = subNode.getKey();
			if (key == null)
				continue;
			patterns.add(key);
		}
		return patterns;
	}

	@Override
	public boolean canCreateWith(Node node) {
		if (!(node instanceof SectionNode))
			return false;
		String key = node.getKey();
		if (key == null)
			return false;
		key = ScriptLoader.replaceOptions(key);
		return getKey().equalsIgnoreCase(key);
	}

}
