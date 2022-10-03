package com.autonomousapps.task

import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class CreateBaselineTask : DefaultTask() {

  init {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Generates baseline of best practices violations"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val report: RegularFileProperty

  @get:Optional
  @get:OutputFile
  abstract val baseline: RegularFileProperty

  @TaskAction
  fun action() {
    val baseline = baseline.getAndDelete()
    report.get().asFile.copyTo(baseline, overwrite = true)
  }
}