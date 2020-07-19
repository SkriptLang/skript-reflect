# Code Conventions

## Separate complex Skript expressions from skript-reflect calls

Combining Skript expressions with skript-reflect calls may make your code difficult to read. Use variables to separate these different types of calls.

{% hint style="danger" %}
```text
(the player's targeted block).breakNaturally()
```
{% endhint %}

{% hint style="success" %}
```text
set {_target} to the player's targeted block
{_target}.breakNaturally()
```
{% endhint %}

## Keep the target of a skript-reflect call grouped

When calling a method or accessing a field, avoid using spaces when possible.

{% hint style="danger" %}
```text
the event.getPlayer()
```
{% endhint %}

{% hint style="success" %}
```text
event.getPlayer()
```
{% endhint %}

If the expression is simple \(i.e. does not contain other expressions\) but requires a space, surround the expression in parentheses.

{% hint style="danger" %}
```text
spawned creeper.isPowered()
```
{% endhint %}

{% hint style="success" %}
```text
(spawned creeper).isPowered()
```
{% endhint %}

If the target of the expression is not simple \(i.e. contains other expressions\), extract the expression into a local variable. \([rule](code-conventions.md#separate-complex-skript-expressions-from-skript-reflect-calls)\)

Variables are the exception to this rule and may contain spaces and/or other expressions

{% hint style="success" %}
```text
{my script::%player%::pet}.isDead()
```
{% endhint %}

## Avoid aliasing classes for aesthetic purposes

The purpose of import aliases is to avoid conflicts with other imports and expressions. Do not alias imports in order to make them look like Skript events.

{% hint style="danger" %}
```text
import:
  org.bukkit.event.player.PlayerMoveEvent as move

on move:
  # code
```
{% endhint %}

{% hint style="success" %}
```text
import:
   org.bukkit.event.player.PlayerMoveEvent

on PlayerMoveEvent:
  # code
```
{% endhint %}

## Avoid unnecessary uses of Java reflection

Especially when copying Java code and translating it for skript-reflect, you may run into instances where you need to use reflection to access a private method, field, or constructor. In skript-reflect, private members are visible and accessible by default.

{% hint style="danger" %}
```text
set {_mod count field} to {_map}.getClass().getDeclaredField("modCount")
{_mod count field}.setAccessible(true)
set {_mod count} to {_mod count field}.get({_map})
```
{% endhint %}

{% hint style="success" %}
```text
set {_mod count} to {_map}.modCount
```
{% endhint %}

