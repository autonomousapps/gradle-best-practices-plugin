package com.autonomousapps.internal.utils

internal object Descriptors {

  // Begins with an 'L'
  // followed by at least one word character
  // followed by one or more word char, /, or $, in any combination
  // ends with a ';'
  // Not perfect, but probably close enough
  private val METHOD_DESCRIPTOR_REGEX = """L\w[\w/$]+;""".toRegex()
  private val DESC_REGEX = """L(\w[\w/$]+);""".toRegex()
  private val FULL_DESC_REGEX = """(L\w[\w/$]+;)""".toRegex()

  // boolean     Z
  // char        C
  // byte        B
  // short       S
  // int         I
  // float       F
  // long        J
  // double      D
  // Object      Ljava/lang/Object;
  // int[]       [I
  // Object[][]  [[Ljava/lang/Object;

  // assertThat(humanReadable("()V")).isEqualTo("()")
  // assertThat(humanReadable("(Ljava/lang/Object;)V")).isEqualTo("(java/lang/Object)")
  // assertThat(humanReadable("()Ljava/util/Set;")).isEqualTo("(): java/util/Set")
  // assertThat(humanReadable("([ILjava/lang/String;)V")).isEqualTo("(int[], java/lang/String)")
  fun humanReadable(descriptor: String): String {
    // TODO implement
    return descriptor
  }
}