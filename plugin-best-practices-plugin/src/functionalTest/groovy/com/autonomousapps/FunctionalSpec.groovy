package com.autonomousapps

import com.autonomousapps.fixture.SimplePluginProject
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

import static com.autonomousapps.fixture.Runner.build
import static com.google.common.truth.Truth.assertThat

final class FunctionalSpec extends Specification {

  @TempDir
  Path tempDir

  def "can check best practices"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when:
    def result = build(project.root, 'checkBestPractices', '-Dbest-practices-logging=quiet')

    then:
    def issues = project.report.readLines()
    assertThat(issues).containsExactly(
      'com.test.GreetingPlugin -> apply -> org.gradle.api.Project#subprojects',
      'com.test.GreetingPlugin -> apply -> org.gradle.api.Project#getSubprojects',
      'com.test.GreetingPlugin -> apply -> org.gradle.api.Project#allprojects',
      'com.test.GreetingPlugin -> apply -> org.gradle.api.Project#getAllprojects',
    )
  }

  def "can check best practices with 'check' task"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when:
    def result = build(project.root, 'check')

    then:
    def issues = project.report.readLines()
    assertThat(issues).containsExactly(
      'com.test.GreetingPlugin -> apply -> org.gradle.api.Project#subprojects',
      'com.test.GreetingPlugin -> apply -> org.gradle.api.Project#getSubprojects',
      'com.test.GreetingPlugin -> apply -> org.gradle.api.Project#allprojects',
      'com.test.GreetingPlugin -> apply -> org.gradle.api.Project#getAllprojects',
    )
  }
}
