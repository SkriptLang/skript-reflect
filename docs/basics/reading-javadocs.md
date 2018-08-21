# Reading Javadocs

Most public APIs and libraries offer documentation in the form of Javadocs. Javadocs outline what features of a library are publicly accessible to developers.

Here are a few links to some commonly referenced Javadocs:  
[Java SE 8 Javadocs](https://docs.oracle.com/javase/8/docs/api/index.html?overview-summary.html)  
[Spigot Javadocs](https://hub.spigotmc.org/javadocs/bukkit/index.html?overview-summary.html)

## Non-public APIs

Javadocs do not describe everything available in a library. Most libraries include private classes, methods, fields, and constructors reserved for internal use. Using skript-mirror, these internal APIs are accessible just like any public API.

### Built-in inspection

skript-mirror has built-in tools for dumping all of the available members of an object. If you need a list of these members, including their return types and input parameters, you can use the [Members](utilities.md#members) expression. If you only need a list of names, you can use the [Member Names](utilities.md#member-names) expression.

