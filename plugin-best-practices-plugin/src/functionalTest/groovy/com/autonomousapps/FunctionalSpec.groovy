package com.autonomousapps


import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path

class FunctionalSpec extends Specification {

  @TempDir
  Path tempDir

  def "test"() {
    given:
    newFile('build.gradle').write('''\
      plugins {
        id 'java-gradle-plugin'
        id 'com.autonomousapps.plugin-best-practices-plugin'
      }
      
      gradlePlugin {
        plugins {
          greeting {
            id = 'com.test.greeting'
            implementationClass = 'com.test.GreetingPlugin'
          }
        }
      }
    '''.stripIndent())

    newFile('src/main/java/com/test/GreetingPlugin.java').write('''\
      package com.test;
      
      import org.gradle.api.Plugin;
      import org.gradle.api.Project;
      
      public class GreetingPlugin implements Plugin<Project> {
        public void apply(Project project) {
          // a comment
        }
      }
    '''.stripIndent())

    when:
    def result = GradleRunner.create()
      .withPluginClasspath()
      .forwardOutput()
    //.withGradleVersion(gradleVersion.version)
      .withProjectDir(tempDir.toFile())
      .withArguments("checkBestPractices", "-s")
    // Ensure this value is true when `--debug-jvm` is passed to Gradle, and false otherwise
      .withDebug(ManagementFactory.getRuntimeMXBean().inputArguments.toString().indexOf("-agentlib:jdwp") > 0)
      .build()

    then:
    result.output.contains('classFiles')
    result.output.contains('GreetingPlugin.class')
  }

  private Path newFile(String path) {
    def file = tempDir.resolve(path)
    Files.createDirectories(file.parent)
    return Files.createFile(file)
  }
}
