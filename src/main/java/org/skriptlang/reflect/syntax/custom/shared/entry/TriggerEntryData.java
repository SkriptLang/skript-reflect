package org.skriptlang.reflect.syntax.custom.shared.entry;

import ch.njol.skript.config.Node;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import org.jetbrains.annotations.Nullable;

public class TriggerEntryData extends org.skriptlang.skript.lang.entry.util.TriggerEntryData {

	public TriggerEntryData(String key, @Nullable Trigger defaultValue, boolean optional) {
		super(key, defaultValue, optional);
	}

	public TriggerEntryData(String key, @Nullable Trigger defaultValue, boolean optional, boolean multiple) {
		super(key, defaultValue, optional, multiple);
	}

	@Override
	public @Nullable Trigger getValue(Node node) {
		ParserInstance parser = ParserInstance.get();
		Node previousNode = parser.getNode();
		try {
			parser.setNode(node);
			return super.getValue(node);
		} finally {
			parser.setNode(previousNode);
		}
	}

}
