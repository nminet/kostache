# kostache

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.8&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://kotlinlang.org/docs/releases.html)

Another kotlin implementation of [Mustache](https://mustache.github.io/mustache.5.html) templates.


## Reference

The reference specification for the Mustache template system is in [Mustache Specification](https://github.com/mustache/spec).
It defines required core modules as well as optional modules.

This implementation includes all core modules as well as the optional *inheritance*, *lambdas* and
*dynamic-names* modules, passing all standard tests.

## API

### Mustache class

The main API is the **Mustache** class, capturing a template and the environment to render data.

```kotlin
import dev.noemi.kostache.Mustache

class Mustache(
    template: String,
    partials: TemplateStore = emptyStore,
    wrap: (Any?) -> Context = KotlinxJsonContext.wrap
) {
    fun render(data: Any? = null): String
}
```

The constructor parses a template and returns an object ready to render data.
**template** must be a valid Mustache template, otherwise **IllegalStateException** is raised.
**partials** indicate how to obtain partials from name.
**wrap** is a callable producing a mustache context from raw data.

The **render** method produces a String by feeding *data* into the template.
if **data** is an instance of **Context** it is used as is. Otherwise, **wrap** is called
to wrap the data for rendering.


### Template class
The **Template** class captures a parse result and can render when given a context.

```kotlin
import dev.noemi.kostache.Template

class Template(
    template: String
) {
    fun render(
        context: Context,
        partials: TemplateStore = emptyStore
    ): String
}
```

**template** must be a valid Mustache template, otherwise **IllegalStateException** is raised.

The exception can be avoided with
```kotlin
val template = Template.load("{{invalid}") ?: Template("fallback")
val result = template.render(KClassContext(null))
check(result == "fallback")
```


### Data wrappers (Context class)
Data wrappers provide an API for mustache to walk through the data to be rendered.
The library provides three implementations of **Context**.


#### KotlinxJsonContext class (default wrapper)
This wrapper uses kotlinx.serialization.json to process JSON data.

```kotlin
val mustache = Mustache(
    template = "hello {{you}}!"
)
val result = mustache.render("""{ "you": "world" }""")
```

- *JsonArray* iterate in sections
- *JsonNull*, *JsonPrimitive* holding a boolean "false", and empty *JsonArray* are *false*.
All other values are *true*.
- *JsonObject* and *JsonPrimitive* process as regular values


Classes annotated with @kotlinx.serialization.Serializable can be rendered

```kotlin
@Serializable
class Widget(val you: String)

val mustache = Mustache(
    template = "hello {{you}}!"
)
val widget = Widget("world")

mustache.render(widget.asJsonElement)
```


#### MapsAndListsContext class
This wrapper takes data from kotlin **Map** and **List** instances.

```kotlin
val mustache = Mustache(
    template = "hello {{you}}!",
    wrap = ::MapsAndListsContext
)
val result = mustache.render(mapOf("you" to "world"))
```

- *List* iterate in sections
- *null*, *Boolean* *false* and empty *List* are *false*. All other values are *true*
- callable *Map* entries with type *() -> String* and *(String) -> String* act as mustache lambdas
- other *Map* entries process as regular values


Processing of map entries that contain Kotlin lambdas depends on position in Mustache source.
- In interpolation position, if the lambda has no parameter and returns a Kotlin String
  it is interpreted as a Mustache lambda. If the lambda has no parameter and does not return
  a String, the result is converted to String and rendered as a Mustache value.
- In section position, if the lambda has one String parameter and returns a String, it is
  interpreted as a Mustache lambda and called with the body of the section. If the lambda
  has zero or one String parameter and does not return a String the lambda is called (with the
  section body if it takes a parameter) and result is interpreted as an object.
- kotlin lambdas in the middle of dotted names are called (passing the section body if it takes
  a parameter) and the result is always interpreted as objects, never as Mustache lambda.


*MapsAndListsContext* is suitable for output from [snakeyaml](https://bitbucket.org/snakeyaml/snakeyaml/src/master).

```kotlin
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

val yamlLoader = Load(LoadSettings.builder().build())
val mustache = Mustache(
    template = "hello {{you}}!",
    wrap = ::MapsAndListsContext
)
val data = yamlLoader.loadFromString("you: world")
val result = mustache.render(data)
```


#### KClassContext class (jvm only)
This wrapper uses reflection to process kotlin classes.

```kotlin
data class Who(val you: String)

val mustache = Mustache(
    template = "hello {{you}}!",
    wrap = ::KClassContext
)
val result = mustache.render(Who("world"))
```

- *List*, *Array* and *Set* iterate in sections
- *false*, *null*, empty *List*, *Array*, *Set* are falsey. All other values are truthy.
- callable public fields (lambdas, methods, "operator fun invoke") with zero or one **String** parameter,
returning a **String** act as mustache lambdas, respectively for values and sections.
- other public fields are converted with *toString* (as per spec, callables are invoked before conversion).
- *Enum* fields can be used as section where only the current value is truthy and evaluates to its name.


#### User defined wrappers
The **Context** class is abstract and can be derived to manage specialized data source as well as adjust the
definition of truth (for historical reasons the mustache specification is imprecise with respect to this).

```kotlin
abstract class Context(
    value: Any?,
    val parent: Context?
) {
    // indicate if the context renders as a regular or inverted section
    abstract fun isFalsey(): Boolean

    // get all child contexts for an iterable section, or null if not iterable
    abstract fun push(): List<Context>?

    // get the context associated to a name for a regular section
    abstract fun push(name: String, body: String?, onto: Context): Context?

    // mustache lambda if available
    open fun asLambda(): String? = null
    
    // text to render
    open fun asValue(): String = value.toString()
}
```

When the *value* passed in constructor is a callable with no parameter, it is invoked and the result
becomes the content's actual value.

Dotted names cause successive calls to **push** - one for each segment of the dotted name.  
When pushing a name in section position, **body** contains the unprocessed text of the section tag. In case
of dotted names all segments receive the same value.  
For names in interpolation position, **body** is not set.


### Template stores

The **TemplateStore** functional interface is used by the rendering process to resolve partials.

```kotlin
    fun interface TemplateStore {
        fun resolve(name: String): Template
    }

    val emptyStore: TemplateStore { _ -> Template() }
```

Two implementations are provided (in addition to *emptyStore*)


#### TemplateFolder class
Look in a directory given by **path** for files with **extension**.

```kotlin
class TemplateFolder(
    path: String,
    extension: String = "mustache"
) : TemplateStore
```

As per Mustache specification, missing templates are handled as null values, not causing error.
If a file is found and contains valid mustache, it is rendered with the current context. If the file
does not contain valid mustache, it is rendered as a text string.

The **TemplateFolder** instance maintains a cache of compiled templates. If the file is modified
on the filesystem and the new contents should be used, the cache can be cleared with

```kotlin
templateFolder.clearCache()
```


#### TemplateMap class
et template from a *Map* of name to mustache source code.

```kotlin
class TemplateMap(
    sourceMap: Map<String, String>
) : TemplateStore
```

An invalid template in *sourceMap* will trigger **IllegalStateException** on construction.


#### TemplateResolver class
Call the supplied function every time a partial is requested.

```kotlin
class TemplateResolver(
    resolver: (String) -> Template
) : TemplateStore
```

The implementation is a straight delegation. It does not maintain any internal state.


## Caveats

IOS/OSX cannot check the type of a kotlin lambda parameter. Because of this, kotlin lambdas
in section position taking one parameter MUST take a **String** parameter (which will receive
the section body).

The Mustache specification does not provide scoping for changes in delimiters inside a file.
In this implementation the change is effective immediately after the delimiters tag and remains
in effect until another change or the end of the file. There is an exception if the change
in delimiters is in the result of a call to a lambda section. In that case the change only affects
the processing of the template produced by the lambda.

The * (asterisk) character following the opening sigil for a partial or parent tag indicates
a dynamic name. In any other position the asterisk is handled as a regular symbol character.


## TODO

Need to do profiling.
- Performance with kotlin-reflect would probably benefit from caching.
- Check it API inhibiting support for lambdas and callable values in 'MapsAndListsContext' has significant benefit.
- Other optimizations?


## Dependencies

The implementation depends on the kotlin standard library, including kotlin reflection and kotlinx serialization.

Noel MINET

2023-02-02
