package com.autonomousapps.task

import com.autonomousapps.internal.logging.ConfigurableLogger
import com.autonomousapps.internal.utils.Json.fromJson
import com.autonomousapps.issue.IssuesReport
import com.autonomousapps.logging.LogLevel
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class CheckBestPracticesTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Checks for violations of Gradle plugin best practices"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val reportJson: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val reportText: RegularFileProperty

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val baseline: RegularFileProperty

  @get:Input
  abstract val projectPath: Property<String>

  @get:Input
  abstract val logLevel: Property<LogLevel>

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.reportJson.set(this@CheckBestPracticesTask.reportJson)
      it.reportText.set(this@CheckBestPracticesTask.reportText)
      it.baseline.set(this@CheckBestPracticesTask.baseline)
      it.projectPath.set(this@CheckBestPracticesTask.projectPath)
      it.logLevel.set(this@CheckBestPracticesTask.logLevel)
    }
  }

  interface Parameters : WorkParameters {
    val reportJson: RegularFileProperty
    val reportText: RegularFileProperty
    val baseline: RegularFileProperty
    val projectPath: Property<String>
    val logLevel: Property<LogLevel>
  }

  abstract class Action : WorkAction<Parameters> {

    private val logger = Logging.getLogger(CheckBestPracticesTask::class.java.simpleName).run {
      ConfigurableLogger(this, parameters.logLevel.get())
    }

    override fun execute() {
      // The project path, unless it's ":", in which case use an empty string
      val projectPath = parameters.projectPath.get().takeUnless { it == ":" } ?: ""
      val baselineFixText = "`./gradlew $projectPath:bestPracticesBaseline`"

      val report = parameters.reportJson.get().asFile
      val issues = report.readText().fromJson<IssuesReport>().issues
      val text = parameters.reportText.get().asFile.readText()
      // Get baseline, if it exists.
      val baseline = parameters.baseline.orNull?.asFile?.readText()?.fromJson<IssuesReport>()?.issues.orEmpty()

      // If we have a baseline, the behavior changes.
      // If we find an issue that isn't in the baseline, it's a new issue.
      val newIssues = issues.filter { it !in baseline }
      // any issue in the baseline that ISN'T also in the list of current issues has been fixed.
      val fixedIssues = baseline.filter { it !in issues }
      val hasNewIssues = newIssues.isNotEmpty()
      val hasFixedIssues = fixedIssues.isNotEmpty()

      // Optionally print to console and throw exception.
      if (issues.isNotEmpty()) {
        logger.report(text)

        if (baseline.isEmpty() || hasNewIssues) {
          val errorText = buildString {
            appendLine("Violations of best practices detected. See the report at ${report.absolutePath} ")
            appendLine()
            appendLine("To create or update the baseline, run $baselineFixText")
          }
          throw GradleException(errorText)
        }
      }

      // Users should maintain their baselines.
      if (hasFixedIssues) {
        throw GradleException("Your baseline contains resolved issues. Update with $baselineFixText")
      }
    }
  }
}