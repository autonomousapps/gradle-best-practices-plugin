package com.autonomousapps.fixture

import java.nio.file.Files
import java.nio.file.Path

final class Project {

  private final Path tempDir

  Project(Path tempDir) {
    this.tempDir = tempDir
    build()
  }

  Path root = tempDir
  Path report = root.resolve('build/reports/best-practices/check.txt')

  private void build() {
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
      import java.util.*;
      
      public class GreetingPlugin implements Plugin<Project> {
        public void apply(Project project) {
          project.subprojects(p -> {
            // a comment
          });
          Set<Project> s = project.getSubprojects();
          
          project.allprojects(p -> {
            // a comment
          });
          Set<Project> a = project.getAllprojects();
        }
      }
    '''.stripIndent())
  }

  private Path newFile(String path) {
    def file = tempDir.resolve(path)
    Files.createDirectories(file.parent)
    return Files.createFile(file)
  }
}
