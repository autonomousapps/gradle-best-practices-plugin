package com.autonomousapps

import com.autonomousapps.logging.LogLevel
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import java.io.File
import javax.inject.Inject

/**
 * ```
 * gradleBestPractices {
 *   // Use this to make it easier to incrementally resolve issues.
 *   // This method accepts a `String`, a `File`, a `RegularFile`, or a `Provider<RegularFile>`.
 *   // default is "$projectDir/best-practices-baseline.json"
 *   baseline '<path/to/file/relative/to/project/dir>'
 *
 *   // default is 'default'
 *   logging '<reporting|debug>'
 * }
 * ```
 */
@Suppress("unused") // intentional API
open class GradleBestPracticesExtension @Inject constructor(
  private val layout: ProjectLayout,
  objects: ObjectFactory
) {

  internal val baseline = objects.fileProperty().convention(layout.projectDirectory.file("best-practices-baseline.json"))
  internal val level = objects.property(LogLevel::class.java).convention(LogLevel.default)

  fun baseline(baseline: Provider<out RegularFile>) {
    this.baseline.set(baseline)
  }

  fun baseline(baseline: RegularFile) {
    this.baseline.set(baseline)
  }

  fun baseline(baseline: File) {
    this.baseline.set(baseline)
  }

  fun baseline(baseline: String) {
    this.baseline.set(layout.projectDirectory.file(baseline))
  }

  /**
   * 1. 'reporting' will emit the report to console (if there are issues).
   * 1. 'debug' will print debug information from the bytecode analysis.
   */
  fun logging(level: LogLevel) {
    this.level.set(level)
  }

  internal companion object {

    internal fun create(project: Project) = project.extensions.create(
      "gradleBestPractices",
      GradleBestPracticesExtension::class.java,
      project.layout,
      project.objects
    )
  }
}
