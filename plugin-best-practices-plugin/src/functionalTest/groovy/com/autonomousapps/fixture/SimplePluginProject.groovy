package com.autonomousapps.fixture

import java.nio.file.Files
import java.nio.file.Path

final class SimplePluginProject {

  private final Path tempDir

  SimplePluginProject(Path tempDir) {
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

    newFile('src/main/java/com/test/GreetingTask.java').write('''\
      package com.test;
      
      import org.gradle.api.DefaultTask;
      import org.gradle.api.Project;
      import org.gradle.api.tasks.TaskAction;
      
      public abstract class GreetingTask extends DefaultTask {
      
        @TaskAction
        public void action() {
        
        }  
      }
    '''.stripIndent())

    newFile('src/main/java/com/test/FancyTask.java').write('''\
      package com.test;
      
      import org.gradle.api.DefaultTask;
      import org.gradle.api.Project;
      import org.gradle.api.tasks.TaskAction;
      
      public abstract class FancyTask extends DefaultTask {
      
        protected abstract void doAction();
      
        @TaskAction
        public void action() {
          doAction();
        }
        
        public static abstract class ReallyFancyTask extends FancyTask {
          @Override
          protected void doAction() {
            getProject().getLogger().quiet("Hello from ReallyFancyTask");
          }
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
