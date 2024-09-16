package io.github.eingruenesbeb.yolo

fun Throwable.localizedMessageWithStackTrace() = "${this.localizedMessage}\n${this.stackTraceToString()}"
