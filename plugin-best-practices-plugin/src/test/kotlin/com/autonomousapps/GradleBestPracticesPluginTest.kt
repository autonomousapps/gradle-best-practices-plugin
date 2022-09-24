package com.autonomousapps

import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class GradleBestPracticesPluginTest {

  @Test fun `plugin registers task`() {
    // Create a test project and apply the plugin
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("java-gradle-plugin")
    project.plugins.apply("com.autonomousapps.plugin-best-practices-plugin")

    // Verify the result
    assertThat(project.tasks.findByName("checkBestPractices")).isNotNull()
  }
}
