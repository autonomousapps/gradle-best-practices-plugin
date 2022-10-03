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
import com.autonomousapps.internal.utils.Json.fromJsonList
import com.autonomousapps.internal.utils.Json.toJson
import com.autonomousapps.internal.utils.filterToClassFiles
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.issue.Issue
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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
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

  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val baseline: RegularFileProperty

  @get:Input
  abstract val creatingBaseline: Property<Boolean>

  @get:Input
  abstract val projectPath: Property<String>

  @get:Input
  abstract val logLevel: Property<ConfigurableLogger.Level>

  @get:OutputFile
  abstract val outputJson: RegularFileProperty

  @get:OutputFile
  abstract val outputText: RegularFileProperty

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.classesDirs.setFrom(classesDirs)
      it.baseline.set(baseline)
      it.creatingBaseline.set(creatingBaseline)
      it.projectPath.set(projectPath)
      it.logLevel.set(logLevel)
      it.outputJson.set(outputJson)
      it.outputText.set(outputText)
    }
  }

  interface Parameters : WorkParameters {
    val classesDirs: ConfigurableFileCollection
    val baseline: RegularFileProperty
    val creatingBaseline: Property<Boolean>
    val projectPath: Property<String>
    val logLevel: Property<ConfigurableLogger.Level>
    val outputJson: RegularFileProperty
    val outputText: RegularFileProperty
  }

  abstract class Action : WorkAction<Parameters> {

    private val logger = Logging.getLogger(CheckBestPracticesTask::class.java.simpleName).run {
      ConfigurableLogger(this, parameters.logLevel.get())
    }

    private val projectPath by lazy {
      val path = parameters.projectPath.get()
      if (path == ":") "" else path
    }

    private val baselineFixText by lazy {
      "`./gradlew $projectPath:bestPracticesBaseline`"
    }

    override fun execute() {
      val outputJson = parameters.outputJson.getAndDelete()
      val outputText = parameters.outputText.getAndDelete()

      val classFiles = parameters.classesDirs.asFileTree.filterToClassFiles().files
      logger.debug("classFiles=${classFiles.joinToString(prefix = "[", postfix = "]")}")

      val issueListener = compositeListener()

      // Visit every class file. Extract information into `issueListener`.
      classFiles.forEach { classFile ->
        classFile.inputStream().use { fis ->
          ClassReader(fis.readBytes()).let { classReader ->
            ClassAnalyzer(issueListener, logger).apply {
              classReader.accept(this, 0)
            }
          }
        }
      }

      // This does a global analysis, so must come after the forEach.
      val issues = issueListener.computeIssues().sortedBy {
        it.javaClass.canonicalName ?: it.javaClass.simpleName
      }

      // Get baseline, if it exists.
      val baseline = parameters.baseline.orNull?.asFile?.readText()?.fromJsonList<Issue>()
      var hasNewIssues = false
      var hasFixedIssues = false

      // Build console text.
      val text = if (baseline.isNullOrEmpty()) {
        // no baseline
        issues.joinToString(separator = "\n\n") { IssueRenderer.renderIssue(it, pretty = true) }
      } else {
        // If we have a baseline, the behavior changes
        // If we find an issue that isn't in the baseline, it's a new issue.
        val newIssues = issues.filter { it !in baseline }
        // any issue in the baseline that ISN'T also in the list of current issues has been fixed.
        val fixedIssues = baseline.filter { it !in issues }
        // any issue in the baseline that IS in the list of current issues is unfixed.
        val unfixedIssues = baseline.filter { it in issues }
        hasNewIssues = newIssues.isNotEmpty()
        hasFixedIssues = fixedIssues.isNotEmpty()

        buildString {
          if (newIssues.isNotEmpty()) {
            appendLine("There are new issues:")
            appendLine(newIssues.joinToString(separator = "\n\n") { IssueRenderer.renderIssue(it, pretty = true) })
            appendLine()
          } else {
            appendLine("No new issues.")
            appendLine()
          }

          if (fixedIssues.isNotEmpty()) {
            appendLine("These issues have been resolved and should be removed from your baseline:")
            appendLine(fixedIssues.joinToString(separator = "\n\n") { IssueRenderer.renderIssue(it, pretty = true) })
            appendLine()
          }

          if (unfixedIssues.isNotEmpty()) {
            appendLine("These issues have been ignored as part of your baseline:")
            appendLine(unfixedIssues.joinToString(separator = "\n\n") { IssueRenderer.renderIssue(it, pretty = true) })
            appendLine()
          }
        }
      }

      // Write output to disk.
      outputText.writeText(text)
      outputJson.writeText(issues.toJson())

      // Optionally print to console and throw exception.
      val isCreatingBaseline = parameters.creatingBaseline.get()
      if (issues.isNotEmpty() && !isCreatingBaseline) {
        logger.report(text)

        if (baseline.isNullOrEmpty() || hasNewIssues) {
          val errorText = buildString {
            appendLine("Violations of best practices detected. See the report at ${outputText.absolutePath} ")
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
