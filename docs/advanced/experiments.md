# Experiments

{% hint style="danger" %}
These features are experimental and are subject to change in the future!
{% endhint %}

In order to enable experimental features, add the following section to your script:

{% code-tabs %}
{% code-tabs-item title="Consent section" %}
```text
skript-mirror, I know what I'm doing:
  I understand that the following features are experimental and may change in the future.
  I have read about this at https://skript-mirror.gitbook.io/docs/advanced/experiments
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Individual features may be enabled by adding the codename of the feature on new lines following the consent section.

## `proxies`

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[a] [new] proxy [instance] of %javatypes% (using|from) %objects%
```
{% endcode-tabs-item %}
{% endcode-tabs %}

The first argument \(`%javatypes%`\) is a list of imported interfaces (whether a class is an interface can be found on the javadoc).

The second argument is an indexed list variable, with each element in the form `{list::%method name%} = %function%`.
`%method name%` is the name of one of the methods from one of the interfaces.
`%function%` is a function reference, which can be created with the following syntax:
{% code-tabs %}
{% code-tabs-item title="Function reference syntax" %}
```text
[the] function(s| [reference[s]]) %strings% [called with [[the] [arg[ument][s]]] %-objects%]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

The first argument \(`%strings%`\) is the name of the function you want to reference. This is enough for the function reference to be completed,
but you can also add some argument values.

When a method from the proxy is ran, it is passed on to the function corresponding to the method name.
The arguments of this function are defined in the following way:
1. The argument values specified in the function reference \(if there are any\)
2. The proxy instance object itself.
3. The argument values from the method call.

Here's an example to help you understand it:
{% code-tabs %}
{% code-tabs-item title="Function reference syntax" %}
```text
import:
	java.lang.Runnable

skript-mirror, I know what I'm doing:
	I understand that the following features are experimental and may change in the future.
	I have read about this at https://skript-mirror.gitbook.io/docs/advanced/experiments
	proxies

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
{% endcode-tabs %}

{% hint style="info" %}
Class proxies are most useful for more interaction with Java code, for example when methods require some implementation of an interface.
{% endhint %}

## `deferred-parsing`

Deferred parsing allows you to prefix any line with `(parse[d] later)` to defer parsing until the first execution of the line. This allows you to circumvent issues where custom syntaxes are used before they are defined.

{% hint style="danger" %}
This should only be used when two custom syntaxes refer to each other. Other issues should be resolved by reordering custom syntax definitions and ensuring that libraries containing custom syntax load before other scripts.
{% endhint %}

