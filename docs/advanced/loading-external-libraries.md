# Loading external libraries

Normally, you may only access classes loaded in the server's classpath, such as Java standard library classes, Bukkit classes, and plugin classes. If you want to use third-party libraries that are not included on the server classpath, you must load them through skript-reflect first.

To load a jar file, place it in `plugins/skript-reflect/` \(create the folder if it doesn't exist\).

Once an external library is loaded, its classes may be imported just like any other class.

