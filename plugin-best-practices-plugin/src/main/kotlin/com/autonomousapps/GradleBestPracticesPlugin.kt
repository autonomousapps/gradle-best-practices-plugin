package com.autonomousapps

import com.autonomousapps.GradleBestPracticesExtension.Companion.BASELINE_DEFAULT
import com.autonomousapps.task.CheckBestPracticesTask
import com.autonomousapps.task.CreateBaselineTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

@Suppress("unused")
class GradleBestPracticesPlugin : Plugin<Project> {

  override fun apply(project: Project): Unit = project.run {
    pluginManager.withPlugin("java-gradle-plugin") {
      val extension = GradleBestPracticesExtension.create(this)

      val mainOutput = extensions.getByType(SourceSetContainer::class.java)
        .findByName(SourceSet.MAIN_SOURCE_SET_NAME)
        ?.output
        ?.classesDirs
        ?: files()

      val baselineTask = tasks.register("bestPracticesBaseline", CreateBaselineTask::class.java)

      val bestPractices = tasks.register("checkBestPractices", CheckBestPracticesTask::class.java) {
        with(it) {
          classesDirs.setFrom(mainOutput)
          baseline.set(extension.baseline)
          creatingBaseline.set(provider { gradle.taskGraph.hasTask(baselineTask.get()) })
          projectPath.set(project.path)
          logLevel.set(extension.level)
          outputJson.set(layout.buildDirectory.file("reports/best-practices/report.json"))
          outputText.set(layout.buildDirectory.file("reports/best-practices/report.txt"))
        }
      }

      baselineTask.configure { t ->
        with(t) {
          baseline.set(extension.baseline.orElse(layout.projectDirectory.file(BASELINE_DEFAULT)))
          report.set(bestPractices.flatMap { it.outputJson })
        }
      }

      tasks.named("check").configure {
        it.dependsOn(bestPractices)
      }
    }
  }
}
