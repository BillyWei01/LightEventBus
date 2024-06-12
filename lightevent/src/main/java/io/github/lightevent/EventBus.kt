package io.github.lightevent

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

/**
 * LightEventBus 是基于 Android 平台的轻量级的事件总线。
 *
 * LightEventBus 的部分实现参考了 greenrobot 的 EventBus。
 * "类名"取“EventBus”是为了尽量代码兼容原版 EventBus 的API。
 *
 * 顾名思义，LightEventBus 相比 EventBus 更加”轻量。
 */
class EventBus {
    companion object {
        private val DEFAULT by lazy { EventBus() }
        private val INSTANT_MAP = ConcurrentHashMap<String, EventBus>()

        /**
         * 获取默认总线实例
         */
        @JvmStatic
        fun getDefault(): EventBus {
            return DEFAULT
        }

        /**
         * 获取总线实例
         *
         * - 不同的逻辑单元/模块可以创建不同的总线。
         * - 不同的总线之间不会收到彼此的事件。
         * - 暂时不提供“删除总线”的API。
         *   毕竟EventBus实例就几个容器，不引用订阅者的话不会占用太多内容。
         *   而订阅者支持取消订阅（unregister)。
         *
         * @param channel 通道，即总线实例的key。
         */
        @JvmStatic
        fun get(channel: String): EventBus {
            if (channel.isEmpty()) return DEFAULT
            return INSTANT_MAP[channel] ?: synchronized(this) {
                INSTANT_MAP[channel] ?: EventBus().also { INSTANT_MAP[channel] = it }
            }
        }
    }

    private class PostingThreadState {
        var eventQueue: ArrayDeque<Any>? = null
        var isPosting = false
    }

    private val postingCount = AtomicInteger()

    private val currentPostingThreadState: ThreadLocal<PostingThreadState> =
        object : ThreadLocal<PostingThreadState>() {
            override fun initialValue(): PostingThreadState {
                return PostingThreadState()
            }
        }

    private val mainPoster = Handler(Looper.getMainLooper())
    private val backgroundPoster = Poster(1)
    private val asyncPoster = Poster(8)

    // 事件 -> 订阅者（集合）
    private val subscriptions = mutableMapOf<Class<*>, MutableList<EventHandler<*>>>()

    // 粘性事件
    private val stickEvents = ConcurrentHashMap<Class<*>, Any>()

    /**
     * 订阅
     */
    fun register(handlers: List<EventHandler<*>>) {
        synchronized(this) {
            handlers.forEach { handler ->
                val eventType = handler.eventType
                val handlerList = subscriptions.getOrPut(eventType) { ArrayList(2) }
                // 如果没有线程正在访问方法列表，则直接添加;
                // 如果有，则执行 CopyOnWrite
                if (postingCount.get() == 0) {
                    handlerList.add(handler)
                } else {
                    subscriptions[eventType] = handlerList.toMutableList().apply { add(handler) }
                }
            }
        }

        // 如 handler 标记了接收粘性事件，且 stickEvents 存在对应的事件，则发送事件
        if (stickEvents.isNotEmpty()) {
            val isMainThread = Looper.getMainLooper() == Looper.myLooper()
            handlers.forEach { handler ->
                if (handler.sticky) {
                    stickEvents[handler.eventType]?.let { event ->
                        postEvent(handler, event, isMainThread)
                    }
                }
            }
        }
    }

    /**
     * 取消订阅
     */
    fun unregister(handlers: List<EventHandler<*>>) {
        synchronized(this) {
            handlers.forEach { handler ->
                val eventType = handler.eventType
                val handlerList = subscriptions[eventType] ?: return@forEach
                if (handlerList.size == 1) {
                    // 如关注事件的集合只有一个handler，并且是当前订阅列表的handler，移除这个列表
                    if (handlerList[0] == handler) {
                        subscriptions.remove(eventType)
                    }
                    return@forEach
                }
                // 若不止一个handler, 则只移除handler即可
                if (postingCount.get() == 0) {
                    handlerList.remove(handler)
                } else {
                    subscriptions[eventType] = handlerList.toMutableList().apply { remove(handler) }
                }
            }
        }
    }

    /**
     * 发送事件
     */
    fun post(event: Any) {
        val postingState = currentPostingThreadState.get()!!
        if (postingState.isPosting) {
            (postingState.eventQueue ?: ArrayDeque<Any>().also { postingState.eventQueue = it }).add(event)
            return
        }
        postingState.isPosting = true
        postingCount.incrementAndGet()

        try {
            val isMainThread = Looper.getMainLooper() == Looper.myLooper()
            postEvents(event, isMainThread)
            var deferEvent = postingState.eventQueue?.removeFirstOrNull()
            while (deferEvent != null) {
                postEvents(deferEvent, isMainThread)
                deferEvent = postingState.eventQueue?.removeFirstOrNull()
            }
        } finally {
            postingState.isPosting = false
            postingCount.decrementAndGet()
        }
    }

    private fun postEvents(event: Any, isMainThread: Boolean) {
        val handlerList = synchronized(this) { subscriptions[event::class.java] } ?: return
        handlerList.forEach { handler ->
            postEvent(handler, event, isMainThread)
        }
    }

    private fun postEvent(handler: EventHandler<*>, event: Any, isMainThread: Boolean) {
        @Suppress("UNCHECKED_CAST")
        val action = handler.action as Action<Any>
        when (handler.threadMode) {
            ThreadMode.POSTING -> {
                action(event)
            }

            ThreadMode.MAIN -> {
                if (isMainThread) {
                    action(event)
                } else {
                    mainPoster.post { action(event) }
                }
            }

            ThreadMode.MAIN_ORDERED -> {
                mainPoster.post { action(event) }
            }

            ThreadMode.BACKGROUND -> {
                if (isMainThread) {
                    backgroundPoster.enqueue(action, event)
                } else {
                    action(event)
                }
            }

            ThreadMode.ASYNC -> {
                asyncPoster.enqueue(action, event)
            }
        }
    }

    fun postSticky(event: Any) {
        stickEvents[event::class.java] = event
        post(event)
    }

    fun removeStickyEvent(event: Any) {
        val eventType = if (event is Class<*>) event else event::class.java
        stickEvents.remove(eventType)
    }

    fun hasStickyEvent(eventType: Class<*>): Boolean {
        return stickEvents.containsKey(eventType)
    }

    fun removeAllStickyEvents() {
        stickEvents.clear()
    }

    /**
     * 设置Executor, 用于执行异步回调（若不设置，则自行创建线程池）
     */
    fun setExecutor(executor: Executor) {
        Poster.setExecutor(executor)
    }
}
