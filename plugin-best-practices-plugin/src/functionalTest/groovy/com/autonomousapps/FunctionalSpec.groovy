package com.autonomousapps

import com.autonomousapps.fixture.SimplePluginProject
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

import static com.autonomousapps.fixture.Runner.build

final class FunctionalSpec extends Specification {

  @TempDir
  Path tempDir

  def "can check best practices with 'checkBestPractices' task"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when:
    build(project.root, 'checkBestPractices', '-Dbest-practices-logging=reporting')

    then:
    project.report.text.trim() == project.expected.trim()
  }

  def "can check best practices with 'check' task"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when:
    build(project.root, 'check')

    then:
    project.report.text.trim() == project.expected.trim()
  }
}
