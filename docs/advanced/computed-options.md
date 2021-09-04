# Computed Options

Skript's options section allows you to create snippets of text that are copied into other sections of your script. This is useful for static text, but does not work well for text that must be derived from dynamic sources, such as variables.

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
option <option name>:
  get:
    # code, required
```
{% endcode-tabs-item %}
{% endcode-tabs %}

After the computed option is defined, it is accessible as `{@<option name>}`  within the same script.

### Section `get`

Code in this section is executed as soon as it is parsed. This section must [return](custom-syntax/expressions.md#return) a value and must not contain delays.

## Using computed options for NMS imports

NMS packages from before Minecraft 1.17 include the Minecraft version, preventing code referencing NMS classes from working across versions.
To get around this, computed options may be used to dynamically generate the proper NMS package.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
import:
  org.bukkit.Bukkit

option nms:
  get:
    set {_nms version} to Bukkit.getServer().getClass().getPackage().getName().split("\.")[3]
    return "net.minecraft.server.%{_nms version}%"

import:
  {@nms}.MinecraftServer
  {@nms}.Item
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% hint style="warning" %}
While this code dynamically generates the appropriate NMS package prefix, it does not guarantee your code will work across versions! Be aware that classes, methods, and fields may change in incompatible ways across versions.
{% endhint %}
