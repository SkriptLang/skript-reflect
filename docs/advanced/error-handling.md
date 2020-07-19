# Error handling

By default, warnings and errors related to your code are logged to the console. Skript-reflect also offers additional tools that give you more control over how errors are handled.

## Suppressing errors

Adding `try` before a Java call prevents errors from being logged to the console.

{% code-tabs %}
{% code-tabs-item title="example.sk" %}
```text
set {_second item in list} to try {_list}.get(1)
try {_connection}.setUseCaches(true)
```
{% endcode-tabs-item %}
{% endcode-tabs %}

If an error occurs, the error object can still be accessed programmatically.

## Programmatic access

In some cases, you may want to handle errors yourself, either to do your own error logging or to perform an alternate task in case of a failure.

### Error object

{% code-tabs %}
{% code-tabs-item title="Syntax" %}
```text
[the] [last] [java] (throwable|exception|error)
```
{% endcode-tabs-item %}
{% endcode-tabs %}

Returns the last error object thrown by a java call. If there was an issue resolving the method or converting its output, it may be a `com.btk5h.skriptmirror.JavaCallException`.

