# Kotlin Builder DSL
A builder pattern code generator for Kotlin leveraging DSL. Boilerplate code no more!

## Features
- Generates builder pattern code in Kotlin taking advantage of Kotlin DSL syntax
- Supports optional parameters

## Getting started
Note: `kapt` is needed to process annotations
Note: starting 0.2.0, artifacts have been renamed (although previous versions with the new artifact is also reuploaded).

### Versions 0.4.0 and above
Starting 0.4.0, all artifacts will be uploaded to Github Packages. Note that old packages will not be reuploaded anymore.

In your application build.gradle, add the repository link and authentication details. For more information, check here: https://docs.github.com/en/packages/guides/configuring-gradle-for-use-with-github-packages#authenticating-to-github-packages

```
allprojects {
    repositories {
        maven {
            url 'https://maven.pkg.github.com/tompee26/KotlinBuilder'
            credentials {
                username = "your_github_username"
                password = "your_personal_access_token"
            }
        }
    }
}

```

And in your module's build.gradle, add this

```
dependencies {
   implementation "com.tompee.kotlinbuilder:runtime:$latest_version"
   kapt "com.tompee.kotlinbuilder:compiler:$latest_version"
}
```

### Versions 0.3.0 and below
All versions below and including 0.3.0 are hosted in jCenter. Since bintray and jCenter are being discontinued, you will no longer be able to do this.

In your build.gradle, add the following dependencies:

```
dependencies {
   implementation "com.tompee.kotlinbuilder:runtime:$latest_version"
   kapt "com.tompee.kotlinbuilder:compiler:$latest_version"
}
```

## How to use it

Define a `class` or `data class` and annotate with `@KBuilder`. `@KBuilder` has an optional `name` parameter where you can specify a custom builder class name. If not set, it is by default the target class appended with `Builder` (e.g. `PersonBuilder`)
```kotlin
@KBuilder
data class Person(val firstName : String, lastName: String, age: Int)
```

Only the parameters defined in the primary constructor will be eligible for setter function generation.
This will generate a builder class with the same package as the target class that allows you to use the builder pattern and Kotlin DSL.

The generator creates two methods of builder instantiation. The first type creates the actual target object by accepting a lambda that allows you to call the builder methods.

```kotlin
val person = PersonBuilder("Benedict", "Cumberbatch", 40) {
    // can set or override all parameters
    firstName { "Michael" }
    lastName { "Bolton" }
    age { 40 }
}
```
The other way returns the good old fashioned builder object.
```kotlin
val personbuilder = PersonBuilder("name", "Cumberbatch", 40)
val person = personBuilder.build()
```

Note: You have to compile the project after creating the target class to allow the processor to generate the code.

## Optional parameters
By default, all parameters defined in the target class are mandatory parameters. Mandatory parameters are required during builder object instantiation. For example, using the `Person` class above, you need to provide the `firstName`, `lastName` and `age` to construct the builder object.

```kotlin
val person = PersonBuilder("Benedict", "Cumberbatch", 40).build()
```

To specify an optional parameter, annotate it with `@Optional`. The catch with optional parameters is that you have to define a default value provider mechanism to let the builder know what to use.

`@Optional` by default supports default value of certain types. When a parameter type is one of the following, then the corresponding default value is assigned when not explicitly overriden.


| Kotlin Type | Default Value     |
|:------------|:------------------|
| Nullable    | null              |
| Enum        | first item        |
| Unit        | Unit              |
| Byte        | 0                 |
| Short       | 0                 |
| Int         | 0                 |
| Long        | 0L                |
| Float       | 0f                |
| Double      | 0.0               |
| Boolean     | false             |
| String      | ""                |
| List        | `emptyList()`     |
|MutableList  | `mutableListOf()` |
| Map         | `emptyMap()`      |
| MutableMap  | `mutableMapOf()`  |
| Array       | `emptyArray()`    |
| Set         | `emptySet()`      |
| MutableSet  | `mutableSetOf()`  |

A default value provider is also available. When defined, it will always use this default value in conjunction with `@Optional`. To create a default value provider, annotate a class with `@Optional.Provides` and implement `DefaultValueProvider<T>`. To optimize resolution of this default value, an `object` can be used instead.

```kotlin
@Optional.Provides
object LastNameProvider : DefaultValueProvider<String> { // Or class if it requires runtime information.
    override fun get() : String {
        return "last_name"
    } 
}
```

Default values are evaluated when build is called (lazily).

Using types other than those above will fail. However, other explicit mechanisms are available for specifying the default value.  

### Nullable
Use `@Optional.Nullable` to explicitly set a nullable type's initial value to null. If the type is not nullable, this will return an error. The builder will set this parameter by default to null.

```kotlin
@KBuilder
data class Person(
    val lastName : String,
    
    @Optional.Nullable
    val firstName: String?
)

val person = PersonBuilder("last_name")
    .firstName { "first_name" }
    .build()
```

### Enum
Use `@Optional.Enumerable` to explicitly set an enum type's initial value. If the type is not an enum, this will return an error. By default, the default value is the first item (index 0). This can be modified by specifying the desired `EnumPosition`

```kotlin
@KBuilder
data class Item(
    val name: String,

    @Optional.Enumerable(EnumPosition.LAST)
    val type : ItemType
)

val item = Item("camera").build()
```

### Value Provider
Use `@Optional.ValueProvider` to explicitly provide a default value provider. `@Optional.ValueProvider` requires an implementation of `DefaultValueProvider<T>` to generate a custom default value during runtime. If the type is not consistent with the provider type, this will return an error. This will override any given `@Optional.Provides`
Note that the default value is evaluated at builder instance creation.

```kotlin
class LastNameProvider : DefaultValueProvider<String> {
    override fun get() : String {
        return "last_name"
    } 
}

@KBuilder
data class Person(
    val lastName: String,
    
    @Optional.ValueProvider(LastNameProvider::class)
    val firstName : String
)

val person = PersonBuilder("last_name")
    .firstName { "first_name" }
    .build()
```

### Kotlin Default Values
Kotlin allows default values at constructors. To leverage this, use `@Optional.Default`.

```kotlin
@KBuilder
data class Person(
    val lastName : String,
    
    @Optional.Default
    val firstName: String = "Benedict"
)

val person = PersonBuilder("last_name")
    .firstName { "first_name" }
    .build()
```

#### Limitation
Kotlin default parameters are not available as metadata nor are represented as a property on the class type. They are only represented as instructions in byte code. Because of this, there is no reliable way to detect if a parameter has a default value at compile time. You need to ensure that all parameters annotated with `@Optional.Default` really has a default value. The generated code can fail if the types don't match but may not be true for all cases.

#### Caution: Default value resolution
Default values are represented as nullable types in the builder. When these nullable variables are modified, they will overwrite the default value upon call to build. However, named non-null parameters in Kotlin does not support null inputs. To work around this, a powerset of default values is generated that checks for all possible combinations of modified default values. The size of this powerset is 2^x where x is the number of default values. The worst case complexity of a builder with default values is therefore Log(2^n).

## Custom setter name
`@Setter` can be used to provide a custom setter name function to a parameter. By default, the parameter name will be used.

```kotlin
@KBuilder
data class Person(
    @Setter("setName")
    val name : String
)

val person = PersonBuilder("name").setName { "newName" }.build()
```

## Coming soon!
1. More default value types
2. More ways to specify default values
3. Set provider arguments to allow `Lazy` evaluation
4. Builder class supertypes

Contributions are welcome!

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
