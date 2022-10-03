package com.autonomousapps.internal.utils

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json as KJson

internal object Json {
  inline fun <reified T> T.toJson(): String {
    return KJson.encodeToString(this)
  }

  inline fun <reified T> String.fromJson(): T {
    return KJson.decodeFromString(this)
  }

  inline fun <reified T> String.fromJsonList(): List<T> {
    return KJson.decodeFromString(this)
  }
}
