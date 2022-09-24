package com.autonomousapps.task

import com.autonomousapps.internal.analysis.AllProjectsListener
import com.autonomousapps.internal.analysis.ClassAnalyzer
import com.autonomousapps.internal.analysis.CompositeIssueListener
import com.autonomousapps.internal.analysis.GetAllprojectsListener
import com.autonomousapps.internal.analysis.GetProjectListener
import com.autonomousapps.internal.analysis.GetSubprojectsListener
import com.autonomousapps.internal.analysis.IssueListener
import com.autonomousapps.internal.analysis.SubprojectsListener
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.logging.ConfigurableLogger
import com.autonomousapps.internal.utils.filterToClassFiles
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.issue.IssueRenderer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
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

  @get:Input
  abstract val logLevel: Property<ConfigurableLogger.Level>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.classesDirs.setFrom(classesDirs)
      it.logLevel.set(logLevel)
      it.output.set(output)
    }
  }

  interface Parameters : WorkParameters {
    val classesDirs: ConfigurableFileCollection
    val logLevel: Property<ConfigurableLogger.Level>
    val output: RegularFileProperty
  }

  abstract class Action : WorkAction<Parameters> {

    private val logger = Logging.getLogger(CheckBestPracticesTask::class.java.simpleName).run {
      ConfigurableLogger(this, parameters.logLevel.get())
    }

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val classFiles = parameters.classesDirs.asFileTree.filterToClassFiles().files
      logger.debug("classFiles=${classFiles.joinToString(prefix = "[", postfix = "]")}")

      val listener = compositeListener()

      // Visit every class file. Extract information into `listener`.
      classFiles.forEach { classFile ->
        classFile.inputStream().use { fis ->
          ClassReader(fis.readBytes()).let { classReader ->
            ClassAnalyzer(listener, logger).apply {
              classReader.accept(this, 0)
            }
          }
        }
      }

      // This does a global analysis, so must come after the forEach.
      val issues = listener.computeIssues().sortedBy {
        it.javaClass.canonicalName ?: it.javaClass.simpleName
      }

      // Write output to disk.
      val text = issues.joinToString(separator = "\n\n") { IssueRenderer.renderIssue(it, pretty = true) }
      output.writeText(text)

      if (issues.isNotEmpty()) {
        logger.report(text)
        throw GradleException("Violations of best practices detected. See the report at ${output.absolutePath} ")
      }
    }

    private fun compositeListener(): IssueListener {
      val listeners = listOf(
        AllProjectsListener(),
        GetAllprojectsListener(),
        SubprojectsListener(),
        GetSubprojectsListener(),
        GetProjectListener(),
      )
      return CompositeIssueListener(listeners)
    }
  }
}
