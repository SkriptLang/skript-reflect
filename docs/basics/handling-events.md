# Handling events

## Listening to events

You may listen to any Bukkit-based event \(including events added by other plugins\) by referencing the imported class. For example, if you wanted to listen to [org.bukkit.event.entity.EnderDragonChangePhaseEvent](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/EnderDragonChangePhaseEvent.html):

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
import:
  org.bukkit.event.entity.EnderDragonChangePhaseEvent

on EnderDragonChangePhaseEvent:
  # your code
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% hint style="warning" %}
Some plugins use their own event handling system or do not pass their events through Bukkit's event executor \(which is the case with some of Skript's internal events\).

In order to listen to an event, it must extend [org.bukkit.event.Event](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/Event.html) and be executed by Bukkit's event executor.
{% endhint %}

You may also listen to multiple events with the same handler. The events do not have to be related, but you should take appropriate precautions if you try to access methods that are available in one event but not in the other. For example, if you want to listen to both [org.bukkit.event.entity.ProjectileLaunchEvent](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/ProjectileLaunchEvent.html) and [org.bukkit.event.entity.ProjectileHitEvent](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/ProjectileHitEvent.html)

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
import:
  org.bukkit.event.entity.ProjectileLaunchEvent
  org.bukkit.event.entity.ProjectileHitEvent

on ProjectileLaunchEvent and ProjectileHitEvent:
  # your code
```
{% endcode-tabs-item %}
{% endcode-tabs %}

## Using the `event` expression

skript-reflect exposes an `event` expression, allowing you to access event values using reflection.

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] event
```
{% endcode-tabs-item %}

{% code-tabs-item title="example.sk" %}
```
import:
  org.bukkit.event.entity.EnderDragonChangePhaseEvent
  org.bukkit.entity.EnderDragon$Phase as EnderDragonPhase

on EnderDragonChangePhaseEvent:
  if event.getNewPhase() is EnderDragonPhase.CIRCLING:
    event.setNewPhase(EnderDragonPhase.CHARGE_PLAYER)
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% hint style="info" %}
The `event` expression may also be used in normal Skript events.
{% endhint %}

## Setting a priority level

The priority level of an event may be set to control when a particular event handler is run relative to other event handlers.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
import:
  org.bukkit.event.entity.EnderDragonChangePhaseEvent

on EnderDragonChangePhaseEvent with priority highest:
  # your code
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Any event priorities defined in [org.bukkit.event.EventPriority](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/EventPriority.html) may be used. Lower priority event handlers are run before higher priority event handlers.

{% code-tabs %}
{% code-tabs-item title="Event Priorities" %}
```text
lowest
low
normal
high
highest
monitor
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% hint style="warning" %}
`highest` is the highest priority event handler you should use if you are modifying the contents of an event. If you only care about the final result of the event, use `monitor`.
{% endhint %}

## Handling cancelled events

By default, event handlers will not be called if an event is cancelled by a lower priority handler. This behavior can be changed by specifying that the handler should handle `all`  events.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
import:
  org.bukkit.event.block.BlockBreakEvent

on all BlockBreakEvent:
  uncancel event
```
{% endcode-tabs-item %}
{% endcode-tabs %}

