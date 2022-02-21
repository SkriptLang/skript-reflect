# Custom syntax

Due to Skript and skript-reflect limitations, it is not easy to create custom syntax through Java calls alone. To help with this, skript-reflect offers utilities that simplify the creation of custom syntax.

## Shared Syntax

### Event Classes

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
event-classes
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Expression

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] expr[ession][s](-| )%number%
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% hint style="info" %}
When used with [the raw expression](../../basics/utilities.md#raw-expression), you can set it to a value, which will change the input value from that argument. This can be used to store data in variables in the calling trigger.
{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
import:
	ch.njol.skript.lang.Variable

effect put %objects% in %objects%:
	parse:
		expr-2 is an instance of Variable # to check if the second argument is a variable
		continue
	trigger:
		set raw expr-2 to expr-1
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endhint %}

### Matched Pattern

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] [matched] pattern
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Parser Mark

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] [parse[r]] mark
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Parse Tag

Parse tags are like parse tags, but they're a set of strings instead of a single integer.

Parse tags were added in Skript 2.6.1, and are explained in more detail [here](https://github.com/SkriptLang/Skript/pull/4176#issuecomment-882056536)
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] parse[r] tags
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Parser Regular Expression

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] [parse[r]] (regex|regular expression)(-| )%number%
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Continue

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
continue
```
{% endcode-tabs-item %}
{% endcode-tabs %}



{% page-ref page="effects.md" %}

{% page-ref page="conditions.md" %}

{% page-ref page="expressions.md" %}

{% page-ref page="events.md" %}
