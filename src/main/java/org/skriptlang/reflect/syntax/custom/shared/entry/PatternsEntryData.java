package org.skriptlang.reflect.syntax.custom.shared.entry;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;

import java.util.ArrayList;
import java.util.List;

public class PatternsEntryData extends EntryData<String[]> {

	public PatternsEntryData(String key, String @Nullable [] defaultValue, boolean optional) {
		super(key, defaultValue, optional);
	}

	@Override
	public String[] getValue(Node node) {
		List<String> patterns = new ArrayList<>();
		for (Node subNode : node) {
			if (subNode instanceof SectionNode) {
				Skript.error("Cannot use a section here.");
				continue;
			}
			String key = ScriptLoader.replaceOptions(subNode.getKey());
			if (key == null)
				continue;
			patterns.add(key);
		}
		return patterns.toArray(new String[0]);
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
