package com.autonomousapps

import com.autonomousapps.task.CheckBestPracticesTask
import com.autonomousapps.task.CreateBaselineTask
import com.autonomousapps.task.DetectBestPracticesViolationsTask
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

      // A RegularFileProperty is allowed to wrap a nullable RegularFile
      @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
      val detectViolationsTask = tasks.register("detectViolations", DetectBestPracticesViolationsTask::class.java) {
        with(it) {
          classesDirs.setFrom(mainOutput)
          baseline.set(extension.baseline.map { f ->
            if (f.asFile.exists()) f else null
          })
          logLevel.set(extension.level)
          outputJson.set(layout.buildDirectory.file("reports/best-practices/report.json"))
          outputText.set(layout.buildDirectory.file("reports/best-practices/report.txt"))
        }
      }

      // A RegularFileProperty is allowed to wrap a nullable RegularFile
      @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
      val checkBestPracticesTask = tasks.register("checkBestPractices", CheckBestPracticesTask::class.java) { t ->
        with(t) {
          reportJson.set(detectViolationsTask.flatMap { it.outputJson })
          reportText.set(detectViolationsTask.flatMap { it.outputText })
          baseline.set(extension.baseline.map { f ->
            if (f.asFile.exists()) f else null
          })
          logLevel.set(extension.level)
          projectPath.set(project.path)
        }
      }

      baselineTask.configure { t ->
        with(t) {
          baseline.set(extension.baseline)
          report.set(detectViolationsTask.flatMap { it.outputJson })
        }
      }

      tasks.named("check").configure {
        it.dependsOn(checkBestPracticesTask)
      }
    }
  }
}
