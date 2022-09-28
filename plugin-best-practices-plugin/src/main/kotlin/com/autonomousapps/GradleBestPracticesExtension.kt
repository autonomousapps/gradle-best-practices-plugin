package com.autonomousapps

import com.autonomousapps.internal.logging.ConfigurableLogger.Level
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * ```
 * gradleBestPractices {
 *   // default is 'default'
 *   logging '<reporting|debug>'
 * }
 * ```
 */
open class GradleBestPracticesExtension @Inject constructor(objects: ObjectFactory) {

  internal val level = objects.property(Level::class.java).convention(Level.default)

  /**
   * 1. 'reporting' will emit the report to console (if there are issues).
   * 1. 'debug' will print debug information from the bytecode analysis.
   */
  fun logging(level: Level) {
    this.level.set(level)
  }

  internal companion object {
    internal fun create(project: Project) = project.extensions.create(
      "gradleBestPractices",
      GradleBestPracticesExtension::class.java,
      project.objects
    )
  }
}
