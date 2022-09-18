package com.autonomousapps.fixture

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import javax.naming.OperationNotSupportedException
import java.lang.management.ManagementFactory
import java.nio.file.Path

class Runner {

  private Runner() {
    throw new OperationNotSupportedException()
  }

  static BuildResult build(
    Path projectDir,
    String... args
  ) {
    return GradleRunner.create()
      .withPluginClasspath()
      .forwardOutput()
      .withProjectDir(projectDir.toFile())
      .withArguments(*args, '-s')
    // Ensure this value is true when `--debug-jvm` is passed to Gradle, and false otherwise
      .withDebug(ManagementFactory.getRuntimeMXBean().inputArguments.toString().indexOf('-agentlib:jdwp') > 0)
      .build()
  }
}
