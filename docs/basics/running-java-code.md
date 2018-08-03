# Running Java code

## Descriptors

In skript-mirror, a descriptor is a name that identifies a particular method or field. A descriptor must contain a name and may optionally contain a declaring class and/or list of parameters.

{% tabs %}
{% tab title="Name Only" %}
{% code-tabs %}
{% code-tabs-item title="Examples" %}
```text
getPlayer
setCancelled
size
```
{% endcode-tabs-item %}
{% endcode-tabs %}

The simplest and most common form of descriptor simply contains the name of a method or field. In most cases, this is enough for skript-mirror to correctly find the method or field you're referring to.
{% endtab %}

{% tab title="Name + Class" %}
{% code-tabs %}
{% code-tabs-item title="Examples" %}
```text
[java.util.HashMap]modCount
[java.util.Random]seed
```
{% endcode-tabs-item %}
{% endcode-tabs %}

A descriptor may specify the declaring class of a field or method before the name.

{% hint style="info" %}
The declaring class must be specified when the field or method is not public.

Since non-public members are not inherited, the declaring class must be provided to proper
{% endhint %}
{% endtab %}

{% tab title="Name + Parameters" %}
{% code-tabs %}
{% code-tabs-item title="Examples" %}
```text
max[int, int]
println[java.lang.Object]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

A descriptor may specify a list of parameters after the name. This is useful when trying to distinguish between similar overloaded methods.
{% endtab %}

{% tab title="Name + Class + Parameters" %}
```text
[java.util.ArrayList]ensureExplicitCapacity[int]
```

A descriptor may contain both a declaring class and a list of parameters.

{% hint style="info" %}
The declaring class must be specified when the field or method is not public.

Since non-public members are not inherited, the declaring class must be provided to properly resolve the member.
{% endhint %}
{% endtab %}
{% endtabs %}

## Calling methods

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
%object%.<descriptor>(%objects%)
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

## Calling fields

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
%object%.<descriptor>!
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% hint style="info" %}
References to fields must end in `!` due to limitations in Skript's parser.
{% endhint %}

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



