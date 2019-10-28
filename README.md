# Kotlin Builder DSL
A builder pattern code generator for Kotlin leveraging DSL. Boilerplate code no more!
Note: Pre-alpha release.

## Features
- Generates builder and factory pattern code in Kotlin taking advantage of DSL
- Supports optional parameters

## Getting started
Note: `kapt` is needed to process annotations

In your build.gradle, add the following dependencies:

```
dependencies {
   implementation "com.tompee.kotlinbuilder:annotations:$latest_version"
   kapt "com.tompee.kotlinbuilder:processor:$latest_version"
}
```

Define a `class` or `data class` and annotate with `@Builder`. `@Builder` has an optional `name` parameter where you can specify a custom builder class name. If not set, it is by default the target class appended with `Builder` (i.e. `PersonBuilder`)
```kotlin
@Builder
data class Person(val name : String)
```

Only the parameters defined in the primary constructor will be eligible for setter function generation.
This will generate a builder class that allows you to use the builder pattern and Kotlin DSL.

## Optional parameters
By default, all parameters defined in the target class are `mandatory` parameters. Mandatory parameters are required during builder object instantiation. For example, using the `Person` class above, you need to provide the `name` to construct the builder object.

```kotlin
val person = PersonBuilder("name").build()
```

To specify an optional parameter, annotate it with `@Optional`. The catch with optional parameters is that you have to define a default value provider mechanism to let the builder know what to use. Currently, there are 2 methods in doing so.

### Nullable
`@Nullable` must be used in conjunction with `@Optional`. `@Nullable` requires that the target parameter type be nullable (i.e. String?). The builder will set this parameter by default to null.

```kotlin
@Builder
data class Person(
    val lastName : String,
    
    @Optional
    @Nullable
    val firstName: String?
)
...
val person = PersonBuilder("last_name")
    .firstName { "first_name" }
    .build()
```

### Provider
`@Provider` must be used in conjunction with `@Optional`. `@Provider` requires an implementation of `DefaultValueProvider<T>` to generate a custom default value of your choice.

```kotlin
class LastNameProvider : DefaultValueProvider<String> {
    override fun get() : String {
        return "last_name"
    }
}

@Builder
data class Person(
    val lastName: String,
    
    @Optional
    @Provider(LastNameProvider::class)
    val firstName : String
)
...
val person = PersonBuilder("last_name")
    .firstName { "first_name" }
    .build()
```

Additional mechanism will be added in the future so watch out.

## Custom setter name
`@Setter` can be used to provide a custom setter name function to a parameter. By default, the parameter name will be used.

```kotlin
@Builder
data class Person(
    @Setter("setName")
    val name : String
)
...
val person = PersonBuilder("name").setName { "newName" }.build()
```

## How it works
The generator creates two methods of builder instantiation. The first type creates the actual target object by accepting a lambda that allows you to call the builder methods.

```kotlin
val person = PersonBuilder("name") { // <- set the optional and even the override the mandatory parameters
    firstName { "Michael" }
    lastName { "Bolton" }
    age { 12 }
}
```
The second way returns the good old fashioned builder object.
```kotlin
val personbuilder = PersonBuilder("name")
```

## Coming soon!
1. More ways to specify default values
2. More robust code generation and error detection
3. Factory pattern? `Lazy` support?

## License
```
MIT License

Copyright (c) 2019 tompee

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
