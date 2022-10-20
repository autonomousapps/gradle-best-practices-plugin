# Gradle Best Practices Plugin

## Add to your project

```
// build.gradle[.kts]
plugins {
  id("com.autonomousapps.plugin-best-practices-plugin") version "<<latest version>>"
}
```

## Use it

```shell
./gradlew :plugin:checkBestPractices
```

Where `:plugin` is the name of your plugin project.

Or, since the `checkBestPractices` task is automatically added as a dependency of the `check` task:

```shell
./gradlew :plugin:check
```

## Example results

The `checkBestPractices` task may print a report such as the following:

```
com.test.GreetingPlugin#apply(Ljava.lang.Object;)V ->
  com.test.GreetingPlugin#apply(Lorg.gradle.api.Project;)V ->
  org.gradle.api.Project#allprojects(Lorg.gradle.api.Action;)V

com.test.FancyTask#action()V ->
  com.test.FancyTask#doAction()V ->
  com.test.FancyTask$ReallyFancyTask#doAction()V ->
  com.test.FancyTask$ReallyFancyTask#getProject()Lorg.gradle.api.Project;
```

This indicates that your plugin is calling `Project#allprojects()`, which violates best practices no matter the context;
and also that it calls `Task#getProject()`, which violates best practices when called from the context of a method
annotated with `@TaskAction`.

## Summary of issues currently detected

### Instances of cross-project configuration

This is dangerous for a variety of reasons. It defeats configuration on demand and will be impermissible in the future
when Gradle implements [project isolation](https://gradle.github.io/configuration-cache/#project_isolation). In the
present, these APIs permit mutation of other projects, and this kind of cross-project configuration can easily lead to
unmaintainable builds.

1. Any usage of `Project#allprojects()`.
2. Any usage of `Project#getAllprojects()`.
3. Any usage of `Project#subprojects()`.
4. Any usage of `Project#getSubprojects()`.

### Usages of a `Project` instance from a task action

This will break the [configuration cache](https://docs.gradle.org/nightly/userguide/configuration_cache.html), since
`Project`s cannot be serialized.

1. Usages of `getProject()` in the context of a method annotated with `@TaskAction`. 
