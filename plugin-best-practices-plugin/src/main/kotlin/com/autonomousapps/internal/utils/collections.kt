package com.autonomousapps.internal.utils

import org.gradle.api.file.FileCollection

/** Filters a [FileCollection] to contain only class files (and not the module-info.class file). */
internal fun FileCollection.filterToClassFiles(): FileCollection {
  return filter {
    it.isFile && it.name.endsWith(".class") && it.name != "module-info.class"
  }
}
