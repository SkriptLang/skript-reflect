package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.elements.StructCustomEvent;

import java.util.List;

/**
 * This class contains some useful methods for dealing with custom events.
 */
public class CustomEventUtils {

	/**
	 * @param which The EventSyntaxInfo that belongs to the used custom event
	 * @param classInfo The used ClassInfo
	 * @return whether the given CustomEvent supports the given ClassInfo as an event-value.
	 */
	public static boolean hasEventValue(EventSyntaxInfo which, ClassInfo<?> classInfo) {
		List<ClassInfo<?>> eventValueClassInfoList = StructCustomEvent.eventValueTypes.get(which);
		if (eventValueClassInfoList == null)
			return false;

		Class<?> classInfoClass = classInfo.getC();
		for (ClassInfo<?> loopedClassInfo : eventValueClassInfoList) {
			if (classInfoClass.isAssignableFrom(loopedClassInfo.getC())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param classInfo The ClassInfo which name is returned
	 * @return Skripts name for the given ClassInfo.
	 */
	public static String getName(ClassInfo<?> classInfo) {
		return Classes.getSuperClassInfo(classInfo.getC()).getName().toString();
	}

	/**
	 * @param which The EventSyntaxInfo that belongs to the used custom event
	 * @return The defined name (identifier) of the custom event from the given EventSyntaxInfo
	 */
	public static String getName(EventSyntaxInfo which) {
		if (which == null)
			return null;

		return StructCustomEvent.nameValues.get(which);
	}

}
