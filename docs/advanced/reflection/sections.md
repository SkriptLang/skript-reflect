# Sections

Sections are very similar to functions: they contain code, they (optionally) have some input variables and (optionally) give some output.
One of the key differences being that sections can be created within a trigger.

## Creating a section

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
create [new] section [with [arguments variables] %-objects%] (and store it|stored) in %objects%
```
{% endcode-tabs-item %}
{% endcode-tabs %}

The argument variables are a list of variables that represent the input of the section.
Example: `with argument variables {_x}, {_y}`.

For section output, you have to use [return](../custom-syntax/expressions.md#return).

The last expression is a variable in which the created section will be stored.

Local variables from before the creation of a section are available in the section itself, but won't modify
the local variables from outside the section.

## Running a section

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
run section %section% [(1¦sync|2¦async)] [with [arguments] %-objects%] [and store [the] result in %-objects%] [(2¦and wait)]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

This effect will run the given section.
If you run the section (a)sync, you can choose to wait for it to be done or not, by appending `and wait` or not.
{% hint style="info" %}
Note that you can't get output from running an async section without waiting for it to return.
{% endhint %}

You can specify arguments with this effect, for example like this: `with arguments {_a}, {_b}`.

The output of the section is stored in the result part: `and store result in {_result}`.

## Example

{% code-tabs %}
{% code-tabs-item title="Example" %}
```text
set {_i} to 2
create new section with {_x} stored in {_section}:
	return {_x} * {_i}
run section {_section} async with 3 and store result in {_result} and wait
broadcast "Result: %{_result}%" # shows 6
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% hint style="info" %}
Sections are very useful to use with [proxies](proxies.md), since you don't have to create functions for the proxy with sections.
{% endhint %}
