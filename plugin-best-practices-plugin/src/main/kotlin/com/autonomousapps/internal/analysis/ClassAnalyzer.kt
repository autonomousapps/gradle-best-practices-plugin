package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.asm.AnnotationVisitor
import com.autonomousapps.internal.asm.ClassVisitor
import com.autonomousapps.internal.asm.MethodVisitor
import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.issue.AllprojectsIssue
import com.autonomousapps.issue.GetAllprojectsIssue
import com.autonomousapps.issue.GetSubprojectsIssue
import com.autonomousapps.issue.Issue
import com.autonomousapps.issue.SubprojectsIssue
import org.gradle.api.logging.Logger

private const val ASM_VERSION = Opcodes.ASM9

internal class ClassAnalyzer(private val logger: Logger) : ClassVisitor(ASM_VERSION) {

  val issues = mutableSetOf<Issue>()

  private val trace = mutableListOf<String>()

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?
  ) {
    logger.quiet("ClassAnalyzer#visit: $name super=$superName")
    trace.add(name.dotty())
  }

  override fun visitEnd() {
    logger.quiet("- visitEnd")
  }

  override fun visitMethod(
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    logger.quiet("- visitMethod: name=$name descriptor=$descriptor signature=$signature access=$access")

    val thisTrace = ArrayList(trace).apply { add(name) }
    return MethodAnalyzer(logger, issues, thisTrace)
  }

  internal class MethodAnalyzer(
    private val logger: Logger,
    private val issues: MutableSet<Issue>,
    private val trace: MutableList<String>
  ) : MethodVisitor(ASM_VERSION) {

    private var isTaskAction = false

    override fun visitEnd() {
      logger.quiet("  - visitEnd")
    }

    // visitAnnotation: descriptor=Lorg/gradle/api/tasks/TaskAction; visible=true
    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
      logger.quiet("  - visitAnnotation: descriptor=$descriptor visible=$visible")

      // TODO look at tasks that have a hierarchy (AGP does this a lot)
      isTaskAction = descriptor == "Lorg/gradle/api/tasks/TaskAction;"
      return null
    }

    // TODO:
    //  See https://stackoverflow.com/questions/47000699/how-to-extract-access-flags-of-a-field-in-asm-visitfield-method
    //  FancyTask has a TaskAction that calls FancyTask#doAction, implemented by FancyTask$ReallyFancyTask, which calls
    //  getProject(), a method that returns Project. I can see that ReallyFancyTask extends FancyTask, so that means the
    //  method doAction() (with identical signature) must override doAction() in the super. From there I can make a
    //  connection.
    //  =>
    //  ClassAnalyzer#visit: com/test/FancyTask$ReallyFancyTask super=com/test/FancyTask
    //  - visitMethod: doAction; ()V signature=null access=4 (4 == protected)
    //    - visitMethodInsn: owner=com/test/FancyTask$ReallyFancyTask name=getProject descriptor=()Lorg/gradle/api/Project;
    //  =>
    //  ClassAnalyzer#visit: com/test/FancyTask super=org/gradle/api/DefaultTask
    //  - visitMethod: name=doAction descriptor=()V signature=null access=1028 (1028 == abstract)
    //  - visitMethod: action; ()V signature=null access=1
    //    - visitAnnotation: descriptor=Lorg/gradle/api/tasks/TaskAction; visible=true
    //    - visitMethodInsn: owner=com/test/FancyTask name=doAction descriptor=()V
    override fun visitMethodInsn(
      opcode: Int,
      owner: String,
      name: String,
      descriptor: String?,
      isInterface: Boolean
    ) {
      logger.quiet("  - visitMethodInsn: owner=$owner name=$name descriptor=$descriptor opcode=$opcode")

      val thisTrace = ArrayList(trace).apply {
        add("${owner.dotty()}#$name")
      }

      val issue = if (owner == "org/gradle/api/Project") {
        when (name) {
          "subprojects" -> SubprojectsIssue(name, thisTrace)
          "getSubprojects" -> GetSubprojectsIssue(name, thisTrace)
          "allprojects" -> AllprojectsIssue(name, thisTrace)
          "getAllprojects" -> GetAllprojectsIssue(name, thisTrace)
          else -> null
        }
      } else {
        null
      }

      issue?.let { issues.add(it) }
    }
  }

  // TODO: maybe delete, not used yet
  internal class AnnotationAnalyzer(
    private val logger: Logger
  ) : AnnotationVisitor(ASM_VERSION) {
    override fun visit(name: String?, value: Any?) {
      logger.debug("    - visit: name=$name value=$value")
    }

    override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor {
      logger.debug("    - visitAnnotation: name=$name descriptor=$descriptor")
      return AnnotationAnalyzer(logger)
    }

    override fun visitArray(name: String?): AnnotationVisitor {
      logger.debug("    - visitArray: name=$name")
      return AnnotationAnalyzer(logger)
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
      logger.debug("    - visitEnum: name=$name descriptor=$descriptor value=$value")
    }

    override fun visitEnd() {
      logger.debug("    - visitEnd")
    }
  }
}

internal fun String.dotty(): String = replace('/', '.')
