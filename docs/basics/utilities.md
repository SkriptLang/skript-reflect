# Utilities

## Collect

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[%objects%]
[%objects% as %javatype%]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Creates an array containing the specified objects. Specifying a type determines the component type of the resulting array.

{% hint style="info" %}
The brackets in this syntax are literal, not representing an optional group.
{% endhint %}

## Spread

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
...%object%
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Returns the contents of a single array, iterable, iterator, or stream.

## Array Value

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
%array%[%integer%]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Represents the value at a certain index of an array.

This value may be read from and written to.

{% hint style="info" %}
The brackets in this syntax are literal, not representing an optional group.
{% endhint %}

## Null

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
null
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Represents `null` in Java. This is different from Skript's `<none>`.

## Bits

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] (bit %number%|bit(s| range) [from] %number%( to |[ ]-[ ])%number%) of %numbers%
%numbers%'[s] (bit %number%|1¦bit(s| range) [from] %number%( to |[ ]-[ ])%number%)
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Represents a subset of bits from a number.

This value may be read from and written to.

## Raw Expression

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] raw %objects%
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Returns the underlying object of an expression.

## Members

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] (fields|methods|constructors) of %objects%
%objects%'[s] (fields|methods|constructors)
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Returns a list of the fields, methods, or constructors of an object, including their modifiers and parameters.

If you need a list of field or method names without modifier or parameter details, see [Member Names](utilities.md#member-names).

## Member Names

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] (field|method) names of %objects%
%objects%'[s] (field|method) names
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Returns a list of the fields or methods of an object.

## Is Instance

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
%objects% (is|are) [a[n]] instance[s] of %javatypes%
%objects% (is not|isn't|are not|aren't) [a[n]] instance[s] of %javatypes%
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Checks whether objects are instances of the given java types.

