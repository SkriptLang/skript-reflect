# Proxies

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[a] [new] proxy [instance] of %javatypes% (using|from) %objects%
```
{% endcode-tabs-item %}
{% endcode-tabs %}

The first argument \(`%javatypes%`\) is a list of imported interfaces (whether a class is an interface can be found on the javadoc).

The second argument is an indexed list variable, with each element in the form `{list::%method name%} = %function/section%`.
`%method name%` is the name of one of the methods from one of the interfaces.
`%function/section%` is either a function reference or a [section](sections.md).

Function wrappers can be created with the following syntax.
{% code-tabs %}
{% code-tabs-item title="Function reference syntax" %}
```text
[the] function(s| [reference[s]]) %strings% [called with [[the] [arg[ument][s]]] %-objects%]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

The first argument \(`%strings%`\) is the name of the function you want to reference. This is enough for the function reference to be completed,
but you can also add some argument values.

When a method from the proxy is ran, it is passed on to the function/section corresponding to the method name.
The arguments are defined in the following way:
1. The argument values specified in the function reference \(if there are any\) \(only if this method doesn't redirect to a section\)
2. The proxy instance object itself.
3. The argument values from the method call.

Here's an example to help you understand it:
{% code-tabs %}
{% code-tabs-item title="Function reference example" %}
```text
import:
    org.bukkit.Bukkit
    ch.njol.skript.Skript
    java.lang.Runnable

function do_something():
    broadcast "It does something!"

command /proxy:
    trigger:
        # As you can see on https://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html
        # the Runnable interface has one method: run
        set {_functions::run} to function reference "do_something"
        set {_proxy} to new proxy instance of Runnable using {_functions::*}
        {_proxy}.run() # will broadcast 'It does something!'
        Bukkit.getScheduler().runTask(Skript.getInstance(), {_proxy}) # also broadcasts 'It does something!'
```
{% endcode-tabs-item %}
{% code-tabs-item title="Section example" %}
```text
import:
	org.bukkit.Bukkit
	ch.njol.skript.Skript
	java.lang.Runnable

command /proxy:
	trigger:
		# As you can see on https://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html
		# the Runnable interface has one method: run
		create section with {_proxy} stored in {_functions::run}:
			broadcast "It does something!"
		set {_proxy} to new proxy instance of Runnable using {_functions::*}
		{_proxy}.run() # will broadcast 'It does something!'
		Bukkit.getScheduler().runTask(Skript.getInstance(), {_proxy}) # also broadcasts 'It does something!'
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% hint style="info" %}
Class proxies are most useful for more interaction with Java code, for example when methods require some implementation of an interface.
{% endhint %}
