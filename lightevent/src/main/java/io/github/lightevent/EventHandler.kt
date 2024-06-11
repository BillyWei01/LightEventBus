package io.github.lightevent

typealias Action<T> = (event: T) -> Unit

/**
 * 事件处理
 *
 * @param eventType 关联的事件类型
 * @param sticky 订阅时是否处理粘性事件（如果有的话）
 * @param threadMode 线程模式
 * @param action 响应事件的方法
 */
class EventHandler<T>(
    val eventType: Class<*>,
    val sticky: Boolean,
    val threadMode: ThreadMode,
    val action: Action<T>
) {
    companion object {
        // 通过 create 方法可以更方便的创建 EventHandler 实例
        inline fun <reified T> create(
            sticky: Boolean = false,
            threadMode: ThreadMode = ThreadMode.POSTING,
            noinline action: Action<T>
        ): EventHandler<T> {
            return EventHandler(T::class.java, sticky, threadMode, action)
        }
    }
}