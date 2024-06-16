package io.github.lightevent

/**
 * 响应事件的方法
 */
typealias Action<T> = (event: T) -> Unit
