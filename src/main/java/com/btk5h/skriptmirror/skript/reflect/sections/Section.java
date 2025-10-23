package com.btk5h.skriptmirror.skript.reflect.sections;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.Variable;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.bukkit.event.Event;

import java.util.List;

public class Section {

	private final Trigger trigger;
	private final Object variablesMap;
	private final List<Variable<?>> argumentVariables;
	private Object[] output;

	public Section(Trigger trigger, Object variablesMap, List<Variable<?>> argumentVariables) {
		this.trigger = trigger;
		this.variablesMap = variablesMap;
		this.argumentVariables = argumentVariables;
	}

	public Section(Trigger trigger, Event event, List<Variable<?>> argumentVariables) {
		this(trigger, SkriptReflection.copyLocals(SkriptReflection.getLocals(event)), argumentVariables);
	}

	public SectionEvent run(Object[][] arguments) {
		output = null;
		SectionEvent sectionEvent = new SectionEvent(this);
		SkriptReflection.putLocals(SkriptReflection.copyLocals(variablesMap), sectionEvent);

		for (int i = 0; i < arguments.length && i < argumentVariables.size(); i++) {
			Object[] currentArg = arguments[i];
			if (currentArg.length == 0) {
				argumentVariables.get(i).change(sectionEvent, null, Changer.ChangeMode.DELETE);
			} else {
				argumentVariables.get(i).change(sectionEvent, currentArg, Changer.ChangeMode.SET);
			}
		}

		TriggerItem.walk(trigger, sectionEvent);
		SkriptReflection.removeLocals(sectionEvent);
		return sectionEvent;
	}

	public Object[] getOutput() {
		return output;
	}

	public void setOutput(Object[] output) {
		this.output = output;
	}

}
