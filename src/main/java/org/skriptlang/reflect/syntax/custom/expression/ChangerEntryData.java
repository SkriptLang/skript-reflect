package org.skriptlang.reflect.syntax.custom.expression;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;

import java.util.Arrays;

public class ChangerEntryData extends EntryData<ChangerNode> {

	public ChangerEntryData(String key, boolean optional) {
		super(key, null, optional);
	}

	@Override
	public ChangerNode getValue(Node node) {
		String key = node.getKey();
		assert key != null;
		key = ScriptLoader.replaceOptions(node.getKey());
		String rawTypes = key.substring(getKey().length()).trim();
		if (rawTypes.isEmpty())
			return new ChangerNode(key, (SectionNode) node, new Class[] {Object.class});

		Class<?>[] acceptedClasses = Arrays.stream(rawTypes.split(","))
			.map(String::trim)
			.map(SkriptUtil::getUserClassInfoAndPlural)
			.map(meta -> {
				Class<?> type = meta.getFirst().getC();
				boolean plural = meta.getSecond();

				return plural ? type.arrayType() : type;
			})
			.toArray(Class[]::new);
		return new ChangerNode(key, (SectionNode) node, acceptedClasses);
	}

	@Override
	public boolean canCreateWith(Node node) {
		if (!(node instanceof SectionNode))
			return false;
		String key = node.getKey();
		if (key == null)
			return false;
		key = ScriptLoader.replaceOptions(key);
		return key.startsWith(getKey());
	}

}
