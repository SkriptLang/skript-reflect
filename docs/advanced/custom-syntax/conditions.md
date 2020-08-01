# Conditions

{% tabs %}
{% tab title="With one pattern" %}
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[local] condition <pattern>:
  usable in:
    # events, optional
  parse:
    # code, optional
  check:
    # code, required
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endtab %}

{% tab title="With multiple patterns" %}
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[local] condition:
  usable in:
    # events, optional
  patterns:
    # patterns, one per line
  parse:
    # code, optional
  check:
    # code, required
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endtab %}

{% tab title="Property condition" %}
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[local] <skript type> property condition <pattern>:
  usable in:
    # events, optional
  parse:
    # code, optional
  check:
    # code, required
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endtab %}
{% endtabs %}

### Flag `local`

Specifying that a condition is `local` makes the condition only usable from within the script that it is defined in. This allows you to create condition that do not interfere with conditions from other addons or scripts.

{% hint style="info" %}
Local conditions are guaranteed to be parsed before other custom conditions, but not necessarily before conditions from other addons.
{% endhint %}

### Section `usable in`

Each entry in this section should be either an imported class or a custom event \(syntax: `custom event %string%`\).

This condition will error if it is used outside of all the given events.

### Section `parse`

Code in this section is executed whenever the condition is parsed. This section may be used to emit errors if the condition is used in an improper context.

If this section is included, you must also [`continue`](README.md#continue) if the effect was parsed successfully.

{% hint style="info" %}
Local variables created in this section are copied by-value to other sections.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
condition example:
  parse:
    set {_test} to 1
    continue
  check:
    # {_test} always starts at 1 here
    add 1 to {_test}
    # 2 is always broadcast
    broadcast "%{_test}%"
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endhint %}

### Section `check`

Code in this section is executed whenever the condition is checked. This section must [`continue`](README.md#continue) if the condition is met. The section may exit without continuing if the condition fails.

