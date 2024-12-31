package com.btk5h.skriptmirror.skript.reflect.sections;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SectionEvent extends Event {

	private final Section section;
	private Object[] output;

	public SectionEvent(Section section) {
		this.section = section;
	}

	public Section getSection() {
		return section;
	}

	public Object[] getOutput() {
		return output;
	}

	public void setOutput(Object[] output) {
		this.output = output;
	}

	@Override
	public HandlerList getHandlers() {
		throw new IllegalStateException();
	}

}
