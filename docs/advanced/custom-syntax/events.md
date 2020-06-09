# Events

{% tabs %}
{% tab title="With one pattern" %}
{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[local] [custom] event <pattern>:
  name: # unique name, required
  event-values: # list of types, optional
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
[local] [custom] event:
  patterns:
    # patterns, one per line
  name: # unique name, required
  event-values: # list of types, optional
  parse:
    # code, optional
  check:
    # code, optional
```
{% endcode-tabs-item %}
{% endcode-tabs %}
{% endtab %}
{% endtabs %}

### Flag `local`

Specifying that an event is `local` makes the event only usable from within the script that it is defined in. This allows you to create events that do not interfere with events from other addons or scripts.

{% hint style="info" %}
Local events are guaranteed to be parsed before other custom events, but not necessarily before events from other addons.
{% endhint %}

### Option `name`

The name you specify here should be used for [`calling the event`](#calling-the-event).

### Option `event-values`

The event-values specified here will be available in the event, either as a default expression \(`message "Hello"` without the need for `to player`\) or as a normal event-value \(`event-player` / `player`\)

### Section `parse`

Code in this section is executed whenever the event is parsed. This section may be used to emit errors if the effect is used in an improper context.

If this section is included, you must also [`continue`](./#continue) if the event was parsed successfully.

{% hint style="info" %}
Local variables created in this section are copied by-value to other sections.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
event example:
  name: example
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

Code in this section is executed just before the event is called. This section may be used to stop the event from being called if certain conditions are met.

If this section is included, you must also [`continue`](./#continue) if you want to event to be called.

### Calling the event

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
call custom event %string% [(with|using) [[event-]values] %-objects%] [[and] [(with|using)] data %-objects%]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

The first argument should contain the name of the event you want to call. The second argument is a list variable, with each element of the following format: `{list::%type%} = %value%`. The second argument is almost the same, the only difference is that `%type%` is replaced with a string, which is just the index.
The first list variable is for [the event-values](#option-event-values), while the second is for [the extra data](#extra-data).

### Extra data

If the event-values aren't enough for your desire, you can make use of the extra data feature.
The syntax for adding event-values to a custom event is explained in [the event-values option](#option-event-values), and how to call an event with them is explained in [calling the event](#calling-the-event)
In the event itself, you can get the extra data with the data expression:

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[extra] [event[-]] data %strings%
```
{% endcode-tabs-item %}
{% endcode-tabs %}
In the syntax above, `%strings%` is the index. This doesn't have to be plural, but can be.

{% hint style="info" %}
It may look fancier to create a custom expression instead of using extra data. To do so, you need to call `event.getData(%index%)` to get the data value.
{% endhint %}
