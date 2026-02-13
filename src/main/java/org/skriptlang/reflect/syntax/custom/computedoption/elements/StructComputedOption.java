package org.skriptlang.reflect.syntax.custom.computedoption.elements;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ReturnHandler;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.structures.StructOptions;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.reflect.syntax.custom.computedoption.ComputedOptionGetEvent;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.SectionEntryData;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StructComputedOption extends Structure {

	private final Priority PRIORITY = StructOptions.PRIORITY;

	public static void register(SyntaxRegistry registry) {
		registry.register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(StructComputedOption.class)
			.supplier(StructComputedOption::new)
			.addPattern("option <.+>")
			.entryValidator(EntryValidator.builder()
				.addEntryData(new SectionEntryData("get", null, false))
				.missingRequiredEntryMessage(key -> "Computed options don't work without a get section")
				.build())
			.build());
	}

	private String option;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {
		option = parseResult.regexes.getFirst().group();
		SectionNode node = entryContainer.get("get", SectionNode.class, false);
		ComputedOption holder = new ComputedOption();

		getParser().setCurrentEvent("computed option getter", ComputedOptionGetEvent.class);
		Trigger trigger = holder.loadReturnableTrigger(node, "get @{" + option + "}", new SimpleEvent());
		computeOption(option, trigger);
		return true;
	}

	@Override
	public boolean load() {
		return true;
	}

	@Override
	public void unload() {
		SkriptReflection.getOptions(getParser().getCurrentScript()).clear();
	}

	private void computeOption(String option, Trigger getter) {
		ComputedOptionGetEvent event = new ComputedOptionGetEvent();
		getter.execute(event);
		String result = Arrays.stream(event.output())
			.map(Object::toString)
			.collect(Collectors.joining());
		SkriptReflection.getOptions(getParser().getCurrentScript()).put(option, result);
	}

	@Override
	public Priority getPriority() {
		return PRIORITY;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "option " + option;
	}

	private static final class ComputedOption implements ReturnHandler<Object> {

		@Override
		public void returnValues(Event event, Expression<?> value) {
			assert event instanceof ComputedOptionGetEvent;
			((ComputedOptionGetEvent) event).output(value.getArray(event));
		}

		@Override
		public boolean isSingleReturnValue() {
			return false;
		}

		@Override
		public Class<?> returnValueType() {
			return Object.class;
		}

	}

}
