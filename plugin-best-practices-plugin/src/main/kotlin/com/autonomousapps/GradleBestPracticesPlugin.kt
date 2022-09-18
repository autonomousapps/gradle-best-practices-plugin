package com.autonomousapps

import com.autonomousapps.task.CheckBestPracticesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

class GradleBestPracticesPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit = project.run {
    pluginManager.withPlugin("java") {
      val sourceSets = extensions.getByType(SourceSetContainer::class.java)
      val mainOutput = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
        ?.output
        ?.classesDirs
        ?: files()

      tasks.register("checkBestPractices", CheckBestPracticesTask::class.java) {
        with(it) {
          classesDirs.setFrom(mainOutput)
          output.set(layout.buildDirectory.file("check.txt"))
        }
      }
    }
  }
}
