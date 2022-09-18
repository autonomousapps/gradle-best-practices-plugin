package com.autonomousapps.task

import com.autonomousapps.internal.analysis.ClassAnalyzer
import com.autonomousapps.internal.asm.ClassReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class CheckBestPracticesTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Checks for violations of Gradle plugin best practices"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFiles
  abstract val classesDirs: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.classesDirs.setFrom(classesDirs)
      it.output.set(output)
    }
  }

  interface Parameters : WorkParameters {
    val classesDirs: ConfigurableFileCollection
    val output: RegularFileProperty
  }

  abstract class Action : WorkAction<Parameters> {

    private val logger = Logging.getLogger(CheckBestPracticesTask::class.java.simpleName)

    override fun execute() {
      val output = parameters.output.get().asFile.apply { delete() }

      val classFiles = parameters.classesDirs.asFileTree.filterToClassFiles().files
      logger.quiet("classFiles=${classFiles.joinToString(prefix = "[", postfix = "]")}")

      classFiles.forEach { classFile ->
        classFile.inputStream().use {
          val visitor = ClassReader(it.readBytes()).let { classReader ->
            ClassAnalyzer(logger).apply {
              classReader.accept(this, 0)
            }
          }

          visitor.issues.forEach { issue ->
            logger.quiet("Issue: ${issue.description}")
          }
        }
      }

      // TODO print to output file
    }
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
