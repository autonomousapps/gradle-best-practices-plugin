package com.autonomousapps.fixture

import java.nio.file.Files
import java.nio.file.Path

final class SimplePluginProject {

  private final Path tempDir
  private final String logLevel

  SimplePluginProject(Path tempDir, String logLevel = 'default') {
    this.tempDir = tempDir
    this.logLevel = logLevel
    build()
  }

  Path root = tempDir
  Path report = root.resolve('build/reports/best-practices/report.txt')

  private void build() {
    newFile('build.gradle').write("""\
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
      
      gradleBestPractices {
        logging '$logLevel'
      }
    """.stripIndent())

    newFile('src/main/java/com/test/GreetingPlugin.java').write('''\
      package com.test;
      
      import org.gradle.api.Plugin;
      import org.gradle.api.Project;
      import java.util.*;
      
      public class GreetingPlugin implements Plugin<Project> {
      
        private Project project;
      
        public void apply(Project project) {
          this.project = project;
        
          project.subprojects(p -> {
            // a comment
          });
          Set<Project> s = project.getSubprojects();
          
          project.allprojects(p -> {
            // a comment
          });
          Set<Project> a = project.getAllprojects();
          
          foo();
        }
        
        private void foo() {
          bar();
        }
        
        private void bar() {
          project.getLogger().quiet("Foobar!");
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

    newFile('src/main/java/com/test/ParentTask.java').write('''\
      package com.test;
      
      import org.gradle.api.DefaultTask;
      import org.gradle.api.Project;
      import org.gradle.api.tasks.TaskAction;
      
      public abstract class ParentTask extends DefaultTask {
      
        protected abstract void doAction();
      
        @TaskAction
        public void action() {
          foo();
        }
        
        private void foo() {
          bar();
        }
        
        private void bar() {
          doAction();
        }
        
        public static abstract class ChildTask extends ParentTask {
          @Override
          protected void doAction() {
            getProject().getLogger().quiet("Hello from ChildTask");
          }
        }
      }
    '''.stripIndent())

    newFile('src/main/java/com/test/ParentTask2.java').write('''\
      package com.test;
      
      import org.gradle.api.DefaultTask;
      import org.gradle.api.Project;
      import org.gradle.api.tasks.TaskAction;
      
      public abstract class ParentTask2 extends DefaultTask {
      
        protected abstract void doAction();
      
        @TaskAction
        public void action() {
          doAction();
        }
        
        public static abstract class ChildTask2 extends ParentTask2 {
          @Override
          protected void doAction() {
            foo();
          }
        
          private void foo() {
            bar(new int[1], "hello!");
          }
          
          private void bar(int[] ints, String s) {
            getProject().getLogger().quiet("Hello from ChildTask2");
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

  String expected = '''\
    com.test.GreetingPlugin#apply(Ljava.lang.Object;)V ->
      com.test.GreetingPlugin#apply(Lorg.gradle.api.Project;)V ->
      org.gradle.api.Project#allprojects(Lorg.gradle.api.Action;)V
    
    com.test.GreetingPlugin#apply(Ljava.lang.Object;)V ->
      com.test.GreetingPlugin#apply(Lorg.gradle.api.Project;)V ->
      org.gradle.api.Project#getAllprojects()Ljava.util.Set;
    
    com.test.FancyTask#action()V ->
      com.test.FancyTask#doAction()V ->
      com.test.FancyTask$ReallyFancyTask#doAction()V ->
      com.test.FancyTask$ReallyFancyTask#getProject()Lorg.gradle.api.Project;
    
    com.test.ParentTask#action()V ->
      com.test.ParentTask#foo()V ->
      com.test.ParentTask#bar()V ->
      com.test.ParentTask#doAction()V ->
      com.test.ParentTask$ChildTask#doAction()V ->
      com.test.ParentTask$ChildTask#getProject()Lorg.gradle.api.Project;
    
    com.test.ParentTask2#action()V ->
      com.test.ParentTask2#doAction()V ->
      com.test.ParentTask2$ChildTask2#doAction()V ->
      com.test.ParentTask2$ChildTask2#foo()V ->
      com.test.ParentTask2$ChildTask2#bar([ILjava.lang.String;)V ->
      com.test.ParentTask2$ChildTask2#getProject()Lorg.gradle.api.Project;
    
    com.test.GreetingPlugin#apply(Ljava.lang.Object;)V ->
      com.test.GreetingPlugin#apply(Lorg.gradle.api.Project;)V ->
      org.gradle.api.Project#getSubprojects()Ljava.util.Set;
    
    com.test.GreetingPlugin#apply(Ljava.lang.Object;)V ->
      com.test.GreetingPlugin#apply(Lorg.gradle.api.Project;)V ->
      org.gradle.api.Project#subprojects(Lorg.gradle.api.Action;)V
  '''.stripIndent()
}
