package org.skriptlang.reflect.syntax.expression;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.util.NonNullPair;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;

import java.util.Arrays;

public class ChangerEntryData extends EntryData<NonNullPair<SectionNode, Class<?>[]>> {

	public ChangerEntryData(String key, boolean optional) {
		super(key, null, optional);
	}

	@Override
	@Nullable
	public NonNullPair<SectionNode, Class<?>[]> getValue(Node node) {
		String key = node.getKey();
		assert key != null;
		key = ScriptLoader.replaceOptions(node.getKey());
		String rawTypes = key.substring(getKey().length()).trim();
		if (rawTypes.isEmpty())
			return new NonNullPair<>((SectionNode) node, new Class<?>[0]);
		Class<?>[] acceptedClasses = Arrays.stream(rawTypes.split(","))
			.map(String::trim)
			.map(SkriptUtil::getUserClassInfoAndPlural)
			.map(meta -> {
				ClassInfo<?> classInfo = meta.getFirst();
				boolean plural = meta.getSecond();

				if (plural) {
					return CollectionUtils.arrayType(classInfo.getC());
				}

				return classInfo.getC();
			})
			.toArray(Class[]::new);
		return new NonNullPair<>((SectionNode) node, acceptedClasses);
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
