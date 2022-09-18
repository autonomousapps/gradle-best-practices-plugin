package com.autonomousapps

import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class GradleBestPracticesPluginTest {

  @Test fun `plugin doesn't register task`() {
    // Create a test project and apply the plugin
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("com.autonomousapps.greeting")

    // Verify the result
    assertThat(project.tasks.findByName("greeting")).isNull()
  }
}
