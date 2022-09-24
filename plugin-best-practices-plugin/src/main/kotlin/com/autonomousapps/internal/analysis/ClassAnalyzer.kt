package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.asm.AnnotationVisitor
import com.autonomousapps.internal.asm.ClassVisitor
import com.autonomousapps.internal.asm.MethodVisitor
import com.autonomousapps.internal.asm.Opcodes
import org.gradle.api.logging.Logger

private const val ASM_VERSION = Opcodes.ASM9

internal class ClassAnalyzer(
  private val listener: IssueListener,
  private val logger: Logger,
) : ClassVisitor(ASM_VERSION) {

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<String>?
  ) {
    logger.debug("ClassAnalyzer#visit: $name super=$superName")
    listener.visitClass(name, superName, interfaces?.toList() ?: emptyList())
  }

  override fun visitMethod(
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    logger.debug("- visitMethod: name=$name descriptor=$descriptor signature=$signature access=$access")

    listener.visitMethod(name, descriptor)
    return MethodAnalyzer(logger, listener)
  }

  internal class MethodAnalyzer(
    private val logger: Logger,
    private val listener: IssueListener,
  ) : MethodVisitor(ASM_VERSION) {

    override fun visitEnd() {
      listener.visitMethodEnd()
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
      logger.debug("  - visitAnnotation: descriptor=$descriptor visible=$visible")
      listener.visitMethodAnnotation(descriptor)
      return null
    }

    override fun visitMethodInsn(
      opcode: Int,
      owner: String,
      name: String,
      descriptor: String,
      isInterface: Boolean
    ) {
      logger.debug("  - visitMethodInsn: owner=$owner name=$name descriptor=$descriptor opcode=$opcode")
      listener.visitMethodInstruction(owner, name, descriptor)
    }
  }
}
