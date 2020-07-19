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
This should only be used when two custom syntaxes refer to each other. Other issues should be resolved by reordering custom syntax definitions and ensuring that libraries containing custom syntax load before other scripts.
{% endhint %}

