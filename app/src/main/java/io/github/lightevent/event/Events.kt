package io.github.lightevent.event

data class NormalEvent(val time: String)

data class StickyEvent(val time: String)

enum class PostThread {
    MAIN,
    BACKGROUND
}

object RemoveStickyEvent