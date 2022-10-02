package com.autonomousapps.internal.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json as KJson

internal object Json {
  inline fun <reified T> T.toJson(): String {
    return KJson.encodeToString(this)
  }
}
