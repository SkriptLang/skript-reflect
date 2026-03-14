package org.skriptlang.reflect.registration;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Collection;

// TODO Replace with Skript's when 2.15 is released
@ApiStatus.Internal
public final class OriginApplyingSyntaxRegistry implements SyntaxRegistry {

	private final SyntaxRegistry syntaxRegistry;
	private final Origin origin;

	public OriginApplyingSyntaxRegistry(SyntaxRegistry syntaxRegistry, Origin origin) {
		this.syntaxRegistry = syntaxRegistry;
		this.origin = origin;
	}

	@Override
	public @Unmodifiable <I extends SyntaxInfo<?>> Collection<I> syntaxes(Key<I> key) {
		return syntaxRegistry.syntaxes(key);
	}

	@Override
	public <I extends SyntaxInfo<?>> void register(Key<I> key, I info) {
		if (info.origin() == Origin.UNKNOWN) { // when origin is unspecified, add one
			//noinspection unchecked
			info = (I) info.toBuilder().origin(origin).build();
		}
		syntaxRegistry.register(key, info);
	}

	@Override
	public void unregister(SyntaxInfo<?> info) {
		syntaxRegistry.unregister(info);
	}

	@Override
	public <I extends SyntaxInfo<?>> void unregister(Key<I> key, I info) {
		syntaxRegistry.unregister(key, info);
	}

	@Override
	public @Unmodifiable Collection<SyntaxInfo<?>> elements() {
		return syntaxRegistry.elements();
	}

}
