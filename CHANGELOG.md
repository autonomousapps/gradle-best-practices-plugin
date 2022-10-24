Gradle Plugin Best Practices Plugin changelog

# Version 0.8
* Use Kotlin 1.7.20.

# Version 0.7
* [Fixed] Use kotlinx.serialization 1.3.3 instead of 1.4.0 (to sync on Kotlin 1.6.21).

# Version 0.4, 0.5, 0.6
* [New] New `gradleBestPractices` DSL with two configuration options: `logging` and `baseline`. See KDoc for more info.
* Now using externally-published `com.autonomousapps:graph-support:0.1` library.

# Version 0.3
* Changed package `com.autonomousapps.internal.graph` to `com.autonomousapps.internal.graphs` to workaround classpath
  issue with dependency-analysis plugin. I should really publish that package as a standalone artifact...

# Version 0.2
* [New] Fail build if issues are discovered.

# Version 0.1
Initial release.
