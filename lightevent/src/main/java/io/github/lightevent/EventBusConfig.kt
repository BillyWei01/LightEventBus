package io.github.lightevent

/**
 * 事件总线配置
 */
data class EventBusConfig (
    // 是否启用事件继承
    val eventInheritance: Boolean = true
)