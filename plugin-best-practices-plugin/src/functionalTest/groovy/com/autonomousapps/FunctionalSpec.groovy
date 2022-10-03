package com.autonomousapps

import com.autonomousapps.fixture.SimplePluginProject
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

import static com.autonomousapps.fixture.Runner.build
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
    project.report.text.trim() == project.expectedReport.trim()
  }

  def "can check best practices with 'check' task"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when:
    buildAndFail(project.root, 'check')

    then:
    project.report.text.trim() == project.expectedReport.trim()
  }

  def "can create best practices baseline"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when:
    build(project.root, 'bestPracticesBaseline')

    then:
    project.bestPractices.text.trim() == project.expectedBaseline.trim()
  }
}
