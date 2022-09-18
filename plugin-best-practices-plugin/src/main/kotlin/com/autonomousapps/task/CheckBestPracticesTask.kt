package com.autonomousapps.task

import com.autonomousapps.internal.analysis.ClassAnalyzer
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.filterToClassFiles
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
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
      val output = parameters.output.getAndDelete()

      val classFiles = parameters.classesDirs.asFileTree.filterToClassFiles().files
      logger.debug("classFiles=${classFiles.joinToString(prefix = "[", postfix = "]")}")

      val issues = classFiles.flatMap { classFile ->
        classFile.inputStream().use { fis ->
          val visitor = ClassReader(fis.readBytes()).let { classReader ->
            ClassAnalyzer(logger).apply {
              classReader.accept(this, 0)
            }
          }

          visitor.issues.map { it.description() }
        }
      }

      output.writeText(issues.joinToString(separator = "\n"))
      if (issues.isNotEmpty()) {
        logger.quiet("Violations of best practices detected. See the report at ${output.absolutePath}")
      }
    }
  }
}
