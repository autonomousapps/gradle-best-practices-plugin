package com.autonomousapps.internal

import com.autonomousapps.internal.utils.Descriptors.humanReadable
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DescriptorsTest {

  @Test
  fun `human readable descriptors`() {
    assertThat(humanReadable("()V")).isEqualTo("()")
    assertThat(humanReadable("(Ljava.lang.Object;)V")).isEqualTo("(java.lang.Object)")
    assertThat(humanReadable("()Ljava.util.Set;")).isEqualTo("(): java.util.Set")
    assertThat(humanReadable("([ILjava.lang.String;)V")).isEqualTo("(int[], java.lang.String)")
  }
}