package io.github.lightevent

typealias Action<T> = (event: T) -> Unit

/**
 * 事件处理
 *
 * @param eventType 关联的事件类型
 * @param threadMode 线程模式
 * @param sticky 订阅时是否处理粘性事件（如果有的话）
 * @param priority 同时订阅同一个事件的订阅多个方法：
 *                  若优先级不同，数值大者先被处理；
 *                  若优先级相同，先订阅者先被处理。
 * @param action 响应事件的方法
 */
class EventHandler<T>(
    val eventType: Class<*>,
    val threadMode: ThreadMode,
    val sticky: Boolean,
    val priority: Int,
    val action: Action<T>
) {
    companion object {
        // 通过 create 方法可以更方便的创建 EventHandler 实例
        inline fun <reified T> create(
            threadMode: ThreadMode = ThreadMode.POSTING,
            sticky: Boolean = false,
            priority: Int = 0,
            noinline action: Action<T>
        ): EventHandler<T> {
            return EventHandler(T::class.java, threadMode, sticky, priority, action)
        }
    }
}
