# Experiments

{% hint style="danger" %}
These features are experimental and are subject to change in the future!
{% endhint %}

In order to enable experimental features, add the following section to your script:

{% code-tabs %}
{% code-tabs-item title="Consent section" %}
```text
skript-reflect, I know what I'm doing:
  I understand that the following features are experimental and may change in the future.
  I have read about this at https://tpgamesnl.gitbook.io/skript-reflect/advanced/experiments
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Individual features may be enabled by adding the codename of the feature on new lines following the consent section.

## `deferred-parsing`

Deferred parsing allows you to prefix any line with `(parse[d] later)` to defer parsing until the first execution of the line. This allows you to circumvent issues where custom syntaxes are used before they are defined.

{% hint style="danger" %}
This should only be used when two custom syntaxes refer to each other. Other issues should be resolved by reordering custom syntax definitions and ensuring that libraries containing custom syntax load before other scripts, or by using the [preloading feature](#preloading).
{% endhint %}


## Preloading
When preloading is enabled in `config.yml`, custom syntax will be available from any scripts, independent of file names.
Preloading is only available from Skript 2.5-alpha6+, using skript-reflect 2.2-alpha1 or above.

There is one case for which custom syntax can't be preloaded, that is when it has a `parse` section. `parse` sections
can't be used in preloadable syntax, so to still allow for custom syntax to run code when being parsed, there are the
`safe parse` sections. These sections have the same purpose as normal `parse` sections, with a few differences:
- In safe parse sections
  - Functions can't be used.
  - Options (including computed options) can't be used.
  - Some imports can't be used (if they contain options for example).

Because of these differences, custom syntax with `safe parse` sections are preloadable.

{% hint style="warning" %}
Be careful when using custom syntax in `on script load` events, as the custom syntax might not have been fully parsed yet.
{% endhint %}
