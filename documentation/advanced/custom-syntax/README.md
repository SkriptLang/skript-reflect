# Custom syntax

Due to Skript and skript-mirror limitations, it is not easy to create custom syntax through Java calls alone. To help with this, skript-mirror offers utilities that simplify the creation of custom syntax.

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
continue [if (%boolean%|%condition%)]
```
{% endcode-tabs-item %}
{% endcode-tabs %}



{% page-ref page="effects.md" %}

{% page-ref page="conditions.md" %}

{% page-ref page="expressions.md" %}



