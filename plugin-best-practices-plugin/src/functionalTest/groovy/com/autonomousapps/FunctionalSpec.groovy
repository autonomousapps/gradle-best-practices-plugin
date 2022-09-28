package com.autonomousapps

import com.autonomousapps.fixture.SimplePluginProject
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

import static com.autonomousapps.fixture.Runner.buildAndFail

final class FunctionalSpec extends Specification {

  @TempDir
  Path tempDir

  def "can check best practices with 'checkBestPractices' task"() {
    given:
    def project = new SimplePluginProject(tempDir, 'reporting')

    when:
    buildAndFail(project.root, 'checkBestPractices')

    then:
    project.report.text.trim() == project.expected.trim()
  }

  def "can check best practices with 'check' task"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when:
    buildAndFail(project.root, 'check')

    then:
    project.report.text.trim() == project.expected.trim()
  }
}
