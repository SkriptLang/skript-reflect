# Importing classes

Many of skript-mirror's reflection features require a reference to a java class. 

## Importing classes at parse-time \(recommended\)

In most cases, the exact qualified name of the class you need is known without running the script. If this is the case, you should use skript-mirror's import block.

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
import:
    <fully qualified name> [as <alias>]
    # multiple imports may be placed under the import section
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Similar to events, import blocks must be placed at the root of your script \(no indentation before `import`\). Imports must also be placed before the imported classes are referred to in your code, so we recommend you place your imports as far up in your script as possible.

Once you import a class through an import block, skript-mirror will create an expression allowing you to reference the java class by its simple name.

{% hint style="info" %}
To avoid conflicts, expressions created by import blocks are only available to the script that imported them. You must import java classes in each script that uses them.
{% endhint %}

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
import:
    java.lang.System
    
command /example:
    trigger:
        message "%System%" # java.lang.System
        System.out!.println("test");
```
{% endcode-tabs-item %}
{% endcode-tabs %}

In most cases, expressions created by import blocks will not conflict with each other or with other Skript expressions. In cases where the class's simple name conflicts with another expression \(such as with `Player` and `String`\), you must import the class under an alias.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
import:
    java.lang.String as JavaString
    
command /example:
    trigger:
        message JavaString.format("Hello %%s", sender)
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% hint style="info" %}
Aliases must be valid Java identifiers!
{% endhint %}

### Importing NMS classes

Since NMS packages change with each Minecraft version, you should generate the package prefix dynamically. See [Computed Options](../advanced/computed-options.md#using-computed-options-for-nms-imports) for more details.

## Importing classes at runtime

Sometimes, the class reference you need cannot be determined until the script is executed.

### From a fully qualified name

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] [java] class %text%
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
on script load:
    set {Player} to the class "org.bukkit.entity.Player"
    message "%{Player}%" # org.bukkit.entity.Player
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### From an object

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] [java] class[es] of %objects%
%objects%'[s] [java] class[es]
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
command /example:
    executable by: players
    trigger:
        set {Player} to player's class
        message "%{Player}%" # org.bukkit.entity.Player
```
{% endcode-tabs-item %}
{% endcode-tabs %}



