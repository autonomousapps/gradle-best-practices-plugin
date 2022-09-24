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

Or, since the `checkBestPractices` is automatically added as a dependency of the `check` task:

```shell
./gradlew :plugin:check
```

## Example results

The `checkBestPractices` may print a report such as the following:

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
