# Running Java code

## Calling methods

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
%object%.<method name>(%objects%)
```
{% endcode-tabs-item %}

{% code-tabs-item title="example.sk" %}
```
event-block.breakNaturally()
(last spawned creeper).setPowered(true)
player.giveExpLevels({_levels})
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Methods may be used as effects, expressions, and conditions. If used as a condition, the condition will pass as long as the return value of the method is not `false`, `null`, or `0`.

### Calling non-public methods

If the method you're trying to invoke is not public, you may have to prefix the method name with the declaring class in brackets.
Since an object may have a non-public method with the same name in multiple superclasses, you must explicitly specify where to find the method.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
{_arraylist}.[ArrayList]fastRemove(1)
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Calling overloaded methods

Generally, skript-reflect can infer the correct overloaded method to call from the arguments passed at runtime. If you need to use a certain implementation of a method, you may append a comma separated list to the end of the method name surrounded in brackets.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
System.out.println[Object]({_something})

Math.max[int, int](0, {_value})
```
{% endcode-tabs-item %}
{% endcode-tabs %}

## Calling fields

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
%object%.<descriptor>
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Calling non-public fields

If the field you're trying to access is not public, you may have to prefix the field name with the declaring class in brackets.
Since an object may have a non-public field with the same name in multiple superclasses, you must explicitly specify where to find the field.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
{_hashmap}.[HashMap]modCount
```
{% endcode-tabs-item %}
{% endcode-tabs %}

## Calling constructors

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[a] new %javatype%(%objects%)
```
{% endcode-tabs-item %}

{% code-tabs-item title="example.sk" %}
```
new Location(player's world, 0, 0, 0)
```
{% endcode-tabs-item %}
{% endcode-tabs %}



