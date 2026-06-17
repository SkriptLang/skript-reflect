package org.skriptlang.reflect.syntax.custom.shared.entry;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import com.btk5h.skriptmirror.JavaType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.java.elements.structures.StructImport;
import org.skriptlang.reflect.syntax.custom.event.CustomEventManager;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.script.Script;

import java.util.function.Predicate;

public class UsableInEntryData extends EntryData<Predicate<ParserInstance>> {

	public UsableInEntryData(String key, @Nullable Predicate<ParserInstance> defaultValue, boolean optional) {
		super(key, defaultValue, optional);
	}

	public UsableInEntryData(String key, @Nullable Predicate<ParserInstance> defaultValue, boolean optional, boolean multiple) {
		super(key, defaultValue, optional, multiple);
	}

	@Override
	public Predicate<ParserInstance> getValue(Node node) {
		Predicate<ParserInstance> cumulativePredicate = parser -> false;
		Script script = ParserInstance.get().getCurrentScript();
		for (Node subNode : node) {
			if (subNode instanceof SectionNode) {
				Skript.error("Cannot use a section here.");
				continue;
			}
			String eventName = ScriptLoader.replaceOptions(subNode.getKey());
			if (eventName.startsWith("custom event ")) {
				String customEventExpr = eventName.substring("custom event ".length());
				Literal<? extends String> parsed = SkriptParser.parseLiteral(customEventExpr, String.class, ParseContext.EVENT);
				if (parsed == null) {
					Skript.error("Cannot understand custom event name: " + customEventExpr);
					continue;
				}
				String identifier = parsed.getSingle();
				cumulativePredicate = cumulativePredicate.or(parser -> CustomEventManager.isCurrentEvent(parser, identifier));
				continue;
			}
			JavaType javaType = StructImport.lookup(script, eventName);
			if (javaType == null) {
				Skript.error("Cannot understand class '" + eventName + "'. Make sure to import it first.");
				continue;
			}

			Class<?> javaClass = javaType.getJavaClass();
			if (!Event.class.isAssignableFrom(javaClass)) {
				Skript.error(javaClass + " is not a Bukkit event");
				continue;
			}
			cumulativePredicate = cumulativePredicate
				.or(parser -> parser.isCurrentEvent(javaClass.asSubclass(Event.class)));
		}
		return cumulativePredicate;
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
