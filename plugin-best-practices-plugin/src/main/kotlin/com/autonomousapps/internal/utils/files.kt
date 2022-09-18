package com.autonomousapps.internal.utils

import org.gradle.api.file.RegularFileProperty
import java.io.File

/** Resolves the file from the property and deletes its contents, then returns the file. */
internal fun RegularFileProperty.getAndDelete(): File {
  val file = get().asFile
  file.delete()
  return file
}
