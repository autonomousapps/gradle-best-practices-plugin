package com.autonomousapps.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CheckBestPracticesTask : DefaultTask() {

  init {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Checks for violations of Gradle plugin best practices"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFiles
  abstract val classesDirs: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val classFiles = classesDirs.asFileTree.filterToClassFiles().files
    logger.quiet("classFiles=${classFiles.joinToString(prefix = "[", postfix = "]")}")

    // TODO: draw the rest of the fucking owl
  }
}

/**
 * Filters a [FileCollection] to contain only class files (and not the module-info.class file).
 */
internal fun FileCollection.filterToClassFiles(): FileCollection {
  return filter {
    it.isFile && it.name.endsWith(".class") && it.name != "module-info.class"
  }
}
