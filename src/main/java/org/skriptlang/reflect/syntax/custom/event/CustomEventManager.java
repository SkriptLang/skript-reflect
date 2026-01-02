package org.skriptlang.reflect.syntax.custom.event;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class CustomEventManager {

	private static final ClassLoader parent = BukkitCustomEvent.class.getClassLoader();
	private static final Map<String, RegisteredEvent> events = new HashMap<>();

	private CustomEventManager() {
		throw new UnsupportedOperationException();
	}

	public static boolean isEventDefined(String identifier) {
		return events.containsKey(identifier);
	}

	/**
	 * Generates and loads a {@link BukkitCustomEvent} subclass and factory class using the given identifier.
	 * @param identifier the event's identifier
	 * @return a weak reference to an instance of the generated factory class for the event
	 */
	public static WeakReference<RegisteredEvent> defineCustomEvent(String identifier) {
		String baseName = BukkitCustomEvent.class.getCanonicalName() + "$" + identifier;
		String factoryName = baseName + "$Factory";

		DynamicClassLoader loader = new DynamicClassLoader(parent);

		Class<? extends BukkitCustomEvent> eventClass = loader.define(
			baseName,
			generateEventClass(identifier)
		).asSubclass(BukkitCustomEvent.class);

		Class<? extends BukkitCustomEventFactory> factoryClass = loader.define(
			factoryName,
			generateFactoryClass(identifier)
		).asSubclass(BukkitCustomEventFactory.class);
		BukkitCustomEventFactory factory;
		try {
			factory = factoryClass.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		RegisteredEvent event = new RegisteredEvent(loader, eventClass, factory, new HandlerList());
		events.put(identifier, event);

		return new WeakReference<>(event);
	}

	/**
	 * <b>NOTE: Make sure ALL instances of the factory are inaccessible before unloading</b>
	 * @param identifier
	 * @return
	 */
	public static boolean unloadCustomEvent(String identifier) {
		RegisteredEvent event = events.remove(identifier);
		if (event == null)
			return false;

		HandlerList handlerList = event.handlerList();
		for (RegisteredListener listener : handlerList.getRegisteredListeners())
			handlerList.unregister(listener);

		return true;
	}

	public static HandlerList getHandlerList(String identifier) {
		System.out.println("Getting handler list for custom event '" + identifier + "'");
		RegisteredEvent event = events.get(identifier);
		if (event == null)
			throw new IllegalStateException("Attempted to get the handler list of an unregistered event: " + identifier);
		System.out.println("Handler list found: " + event.handlerList() + " (" + event.handlerList().hashCode() + ")");
		return event.handlerList();
	}

	/**
	 * Dynamically generates a {@link BukkitCustomEvent} subclass for the given identifier.
	 * <p>
	 * The generated class has the following structure:
	 * <pre>{@code
	 * public class BukkitCustomEvent$<identifier> extends BukkitCustomEvent {
	 *
	 *     public BukkitCustomEvent$<identifier>() {
	 *         super("<identifier>");
	 *     }
	 *
	 *     @Override
	 *     public HandlerList getHandlers() {
	 *         return CustomManager.getHandlerList(<identifier>)
	 *     }
	 *
	 *     public static HandlerList getHandlerList() {
	 *         return CustomEventManager.getHandlerList(<identifier>);
	 *     }
	 *
	 * }
	 * }</pre>
	 *
	 * @param identifier the event's identifier
	 * @return the bytecode for the generated {@code BukkitCustomEvent$<identifier>} class
	 * @see CustomEventManager#generateFactoryClass(String)
	 */
	private static byte[] generateEventClass(String identifier) {
		String superclassName = Type.getInternalName(BukkitCustomEvent.class);
		String className = superclassName + "$" +  identifier;

		ClassWriter cw = new ClassWriter(0);

		/*
		 * Class definition:
		 * public BukkitCustomEvent$<identifier> {}
		 */
		cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, className, null, superclassName, null);

		/*
		Constructor:
		public BukkitCustomEvent$<identifier>() {
			super("<identifier>")
		}
		 */
		MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		init.visitCode();
		init.visitVarInsn(Opcodes.ALOAD, 0);
		init.visitLdcInsn(identifier);
		init.visitMethodInsn(Opcodes.INVOKESPECIAL, superclassName, "<init>", "(Ljava/lang/String;)V", false);
		init.visitInsn(Opcodes.RETURN);
		init.visitMaxs(2, 1);
		init.visitEnd();


		String handlerListDesc = "L" + Type.getInternalName(HandlerList.class) + ";";
		String stringDesc = "L" + Type.getInternalName(String.class) + ";";
		/*
		Method:
		public HandlerList getHandlers() {
			return CustomEventManager.getHandlerList(<identifier>);
		}
		 */
		MethodVisitor getHandlers = cw.visitMethod(Opcodes.ACC_PUBLIC, "getHandlers", "()" + handlerListDesc, null, null);
		getHandlers.visitCode();
		getHandlers.visitLdcInsn(identifier);
		getHandlers.visitMethodInsn(
			Opcodes.INVOKESTATIC,
			Type.getInternalName(CustomEventManager.class),
			"getHandlerList",
			"(" + stringDesc + ")" + handlerListDesc,
			false
		);
		getHandlers.visitInsn(Opcodes.ARETURN);
		getHandlers.visitMaxs(1, 1);
		getHandlers.visitEnd();

		/*
		Method:
		public static HandlerList getHandlerList() {
			return CustomEventManager.getHandlerList(<identifier>);
		}
		 */
		MethodVisitor getHandlerList = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getHandlerList", "()" + handlerListDesc, null, null);
		getHandlerList.visitCode();
		getHandlerList.visitLdcInsn(identifier);
		getHandlerList.visitMethodInsn(
			Opcodes.INVOKESTATIC,
			Type.getInternalName(CustomEventManager.class),
			"getHandlerList",
			"(" + stringDesc + ")" + handlerListDesc,
			false
		);
		getHandlerList.visitInsn(Opcodes.ARETURN);
		getHandlerList.visitMaxs(1, 0);
		getHandlerList.visitEnd();

		cw.visitEnd();
		return cw.toByteArray();
	}

	/**
	 * Dynamically generates a {@link BukkitCustomEventFactory} class for the {@link BukkitCustomEvent} subclass
	 * corresponding to the given identifier.
	 * <p>
	 * The generated factory class has the following structure:
	 * <pre>{@code
	 * public static class BukkitCustomEvent$<identifier>$Factory implements BukkitCustomEventFactory {
	 *     @Override
	 *     public BukkitCustomEvent create() {
	 *         return new BukkitCustomEvent$<identifier>();
	 *     }
	 * }
	 * }</pre>
	 *
	 * @param identifier the event's identifier
	 * @return the bytecode for the generated {@code BukkitCustomEvent$<identifier>$Factory} class
	 * @see CustomEventManager#generateEventClass(String)
	 */
	private static byte[] generateFactoryClass(String identifier) {
		String bukkitCustomEventName = Type.getInternalName(BukkitCustomEvent.class);
		String outerClass = bukkitCustomEventName + "$" + identifier;
		String factoryClass = outerClass + "$Factory";
		String[] interfaces = new String[] {Type.getInternalName(BukkitCustomEventFactory.class)};

		ClassWriter cw = new ClassWriter(0);

		/*
		 * Class definition:
		 * public BukkitCustomEvent$<identifier>$Factory {}
		 */
		cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, factoryClass, null, Type.getInternalName(Object.class), interfaces);

		/*
		 * Constructor:
		 * public BukkitCustomEvent$<identifier>$Factory() {
		 *     super()
		 * }
		 */
		MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		init.visitCode();
		init.visitVarInsn(Opcodes.ALOAD, 0);
		init.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
		init.visitInsn(Opcodes.RETURN);
		init.visitMaxs(1, 1);
		init.visitEnd();

		/*
		create() method implementation:
		public BukkitCustomEvent$<identifier>() {
			return new BukkitCustomEvent$<identifier>();
		}
		 */
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "create", "()L" + bukkitCustomEventName + ";", null, null);
		mv.visitCode();
		mv.visitTypeInsn(Opcodes.NEW, outerClass);
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, outerClass, "<init>", "()V", false);
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(2, 1);
		mv.visitEnd();

		cw.visitEnd();
		return cw.toByteArray();
	}

	private static class DynamicClassLoader extends ClassLoader {

		public DynamicClassLoader(ClassLoader parent) {
			super(parent);
		}

		public Class<?> define(String name, byte[] bytecode) {
			try {
				Files.write(Path.of("D:\\Minecraft\\Servers\\skript-reflect\\plugins\\Skript\\" + name.substring(name.lastIndexOf('.') + 1) + ".class"), bytecode);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return defineClass(name, bytecode, 0, bytecode.length);
		}

	}

	public record RegisteredEvent(
		DynamicClassLoader loader,
		Class<? extends BukkitCustomEvent> eventClass,
		BukkitCustomEventFactory factory,
		HandlerList handlerList
	) {}

}
