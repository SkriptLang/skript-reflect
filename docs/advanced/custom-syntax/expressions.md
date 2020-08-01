# Expressions

{% tabs %}
{% tab title="With one pattern" %}
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[local] [(plural|non(-|[ ])single))] expression <pattern>:
  return type: <skript type (cannot be a java type)> # optional
  loop of: <text> # optional
  usable in:
    # events, optional
  parse:
    # code, optional
  get:
    # code, optional
  add:
    # code, optional
  set:
    # code, optional
  remove:
    # code, optional
  remove all:
    # code, optional
  delete:
    # code, optional
  reset:
    # code, optional
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endtab %}

{% tab title="With multiple patterns" %}
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[local] [(plural|non(-|[ ])single))] expression:
  patterns:
    # patterns, one per line
  return type: <skript type (cannot be a java type)> # optional
  usable in:
    # events, optional
  parse:
    # code, optional
  get:
    # code, optional
  add:
    # code, optional
  set:
    # code, optional
  remove:
    # code, optional
  remove all:
    # code, optional
  delete:
    # code, optional
  reset:
    # code, optional
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endtab %}

{% tab title="Property expression" %}
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[local] <skript types> property <pattern>:
  return type: <skript type> # optional
  usable in:
    # events, optional
  parse:
    # code, optional
  get:
    # code, optional
  add:
    # code, optional
  set:
    # code, optional
  remove:
    # code, optional
  remove all:
    # code, optional
  delete:
    # code, optional
  reset:
    # code, optional
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Property expressions have two patterns:

```text
[the] <pattern> of %$<skript types>%
%$<skript types>%'[s] <pattern>
```

When property expressions are used, if multiple inputs are passed, the `get` section is called multiple times with each individual input passed as the first expression.

{% hint style="info" %}
`expression-1` is always the object the property belongs to, even when the`[the] <pattern> of %$<skript types>%` form is used.
{% endhint %}
{% endtab %}
{% endtabs %}

### Flag `local`

Specifying that an expression is `local` makes the expression only usable from within the script that it is defined in. This allows you to create expression that do not interfere with expressions from other addons or scripts.

{% hint style="info" %}
Local expressions are guaranteed to be parsed before other custom expressions, but not necessarily before expressions from other addons.
{% endhint %}

### Flag `plural`/`non-single`

Specifying that an expression is `plural` or `non-single` indicates that the expression may return more than one value regardless of context.

#### `$` type modifier

If the expression is single or non-single depending on whether the input is single or non-single, you may prefix the type with a `$`.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
expression uppercase %$strings%:
  # ...
```
{% endcode-tabs-item %}
{% endcode-tabs %}

In the above example, `uppercase "test"` would be single and `uppercase ("hello" and "world")` would be non-single.

### Option `return type`

Specifying a return type restricts the possible values that an expression returns, allowing Skript to potentially resolve type conflicts or perform optimizations.

In most cases, explicitly specifying a return type is unnecessary.

### Option `loop of`

If the expression is non-single, this option specifies an alias that may be used if the expression is looped.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
plural expression test points:
  loop of: point

on script load:
  loop test points:
    # You may use "loop-point" instead of "loop-value" here
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Section `usable in`

Each entry in this section should be either an imported class or a custom event \(syntax: `custom event %string%`\).

This condition will error if it is used outside of all the given events.

### Section `parse`

Code in this section is executed whenever the effect is parsed. This section may be used to emit errors if the effect is used in an improper context.

If this section is included, you must also [`continue`](README.md#continue) if the effect was parsed successfully.

{% hint style="info" %}
Local variables created in this section are copied by-value to other sections.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
expression example:
  parse:
    set {_test} to 1
    continue
  get:
    # {_test} always starts at 1 here
    add 1 to {_test}
    # 2 is always returned
    return {_test}
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endhint %}

### Section `get`

Code in this section is executed whenever the expression's value is read. This section must [return](#return) a value and must not contain delays.

#### Return

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
return [%objects%]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Section `add`/`set`/`remove`/`remove all`/`delete`/`reset`

Code in these sections is executed whenever the expression is changed using Skript's change effect \(or by other means\).

#### Change Value

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] change value[s]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Represents the value \(or values\) that the expression is being changed by.

{% hint style="info" %}
If multiple change values are expected, use the plural form of the expression `change values` instead of the singular `change value`.
{% endhint %}

