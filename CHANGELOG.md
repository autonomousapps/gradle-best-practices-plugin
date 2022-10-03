Gradle Plugin Best Practices Plugin changelog

# Version 0.4, 0.5
* [New] New `gradleBestPractices` DSL with two configuration options: `logging` and `baseline`. See KDoc for more info.
* Now using externally-published `com.autonomousapps:graph-support:0.1` library.

# Version 0.3
* Changed package `com.autonomousapps.internal.graph` to `com.autonomousapps.internal.graphs` to workaround classpath
  issue with dependency-analysis plugin. I should really publish that package as a standalone artifact...

# Version 0.2
* [New] Fail build if issues are discovered.

# Version 0.1
Initial release.
