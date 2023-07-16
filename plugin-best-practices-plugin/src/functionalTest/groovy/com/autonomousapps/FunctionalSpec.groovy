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
    def result = buildAndFail(project.root, 'checkBestPractices')

    then: 'Console output matches expected value'
    result.output.contains project.expectedConsoleOutput

    and: 'File version of console output matches expected value'
    project.consoleReport.text.trim() == project.expectedConsoleReport.trim()

    and: 'Json report matches expected value'
    project.jsonReport.text.trim() == project.expectedJsonReport.trim()
  }

  // Same as above, but using `check` lifecycle task instead.
  def "can check best practices with 'check' task"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when:
    buildAndFail(project.root, 'check')

    then:
    project.consoleReport.text.trim() == project.expectedConsoleReport.trim()
  }

  def "can create best practices baseline"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when:
    build(project.root, 'bestPracticesBaseline')

    then:
    project.baselineReport.text.trim() == project.expectedBaseline.trim()
  }


  def "respects baseline"() {
    given:
    def project = new SimplePluginProject(tempDir)

    when: 'Generate the baseline and then check best practices'
    build(project.root, 'bestPracticesBaseline')
    build(project.root, 'checkBestPractices')

    then: "Build doesn't fail"
    project.baselineReport.text.trim() == project.expectedBaseline.trim()
  }
}
