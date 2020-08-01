# Effects

{% tabs %}
{% tab title="With one pattern" %}
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[local] effect <pattern>:
  usable in:
    # events, optional
  parse:
    # code, optional
  trigger:
    # code, required
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endtab %}

{% tab title="With multiple patterns" %}
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[local] effect:
  usable in:
    # events, optional
  patterns:
    # patterns, one per line
  parse:
    # code, optional
  trigger:
    # code, required
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endtab %}
{% endtabs %}

### Flag `local`

Specifying that an effect is `local` makes the effect only usable from within the script that it is defined in. This allows you to create effects that do not interfere with effects from other addons or scripts.

{% hint style="info" %}
Local effects are guaranteed to be parsed before other custom effects, but not necessarily before effects from other addons.
{% endhint %}

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
effect example:
  parse:
    set {_test} to 1
    continue
  trigger:
    # {_test} always starts at 1 here
    add 1 to {_test}
    # 2 is always broadcast
    broadcast "%{_test}%"
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endhint %}

### Section `trigger`

