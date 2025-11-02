package com.btk5h.skriptmirror;

import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.parser.ParserInstance;
import org.jetbrains.annotations.Nullable;

public class FunctionWrapper {

	private final String name;
	private final Object[] arguments;

	public FunctionWrapper(String name, Object[] arguments) {
		this.name = name;
		this.arguments = arguments;
	}

	public String getName() {
		return name;
	}

	public Object[] getArguments() {
		return arguments;
	}

	@Nullable
	public Function<?> getFunction() {
		// Get current script file name
		String script = null;
		ParserInstance parserInstance = ParserInstance.get();
		if (parserInstance.isActive()) {
			script = parserInstance.getCurrentScript().getConfig().getFileName();
		}

		return Functions.getFunction(name, script);
	}

}
