package io.github.lightevent

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

/**
 * LightEventBus 是基于 Android 平台的轻量级的事件总线。
 *
 * LightEventBus 的实现参考了 greenrobot 的 EventBus,
 * 除了订阅方法的注册方式不一样之外，其功能上基本时兼容原版EventBus。
 *
 * 入口类名沿用“EventBus”而不是“LightEventBus”，是为了尽量代码兼容原版 EventBus 的API。
 */
class EventBus {
    companion object {
        private val defaultEventBus by lazy { EventBus() }
        private val instantMap = ConcurrentHashMap<String, EventBus>()

        private val eventTypesCache: MutableMap<Class<*>, ArrayList<Class<*>>> = HashMap()

        /**
         * 获取默认总线实例
         */
        @JvmStatic
        fun getDefault(): EventBus {
            return defaultEventBus
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
         * @param config 用于初始化实例的配置
         */
        @JvmStatic
        fun get(channel: String): EventBus {
            if (channel.isEmpty()) return defaultEventBus
            return instantMap[channel] ?: synchronized(this) {
                instantMap[channel] ?: EventBus().also { instantMap[channel] = it }
            }
        }
    }

    // 是否启用事件继承
    private var eventInheritance: Boolean = true

    private class PostingThreadState {
        var eventQueue: ArrayDeque<Any>? = null
        var isPosting = false
    }

    private val currentPostingThreadState: ThreadLocal<PostingThreadState> =
        object : ThreadLocal<PostingThreadState>() {
            override fun initialValue(): PostingThreadState {
                return PostingThreadState()
            }
        }

    // 正在发送事件的线程的数量
    private val postingCount = AtomicInteger()

    private val mainPoster = Handler(Looper.getMainLooper())
    private val backgroundPoster = Poster(1)
    private val asyncPoster = Poster(8)

    // 事件 -> 订阅者（集合）
    private val subscriptions = mutableMapOf<Class<*>, ArrayList<EventHandler<*>>>()

    // 粘性事件
    private val stickEvents = ConcurrentHashMap<Class<*>, Any>()

    /**
     * 订阅
     */
    fun register(handlers: List<EventHandler<*>>) {
        synchronized(this) {
            handlers.forEach { handler ->
                val eventType = handler.eventType
                val list = subscriptions.getOrPut(eventType) { ArrayList(2) }
                // 如果没有线程正在访问方法列表，则直接添加;
                // 如果有，则执行 CopyOnWrite
                if (postingCount.get() == 0) {
                    addHandler(list, handler)
                } else {
                    subscriptions[eventType] = ArrayList(list).apply { addHandler(this, handler) }
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

    // 按优先级逆序排列
    private fun addHandler(list: ArrayList<EventHandler<*>>, handler: EventHandler<*>) {
        val size = list.size
        val priority = handler.priority
        // 快速判断：列表为空，或者优先级小于等于列表末尾，则直接插入列表末尾
        if (size == 0 || priority <= list[size - 1].priority) {
            list.add(handler)
        } else {
            for (i in 0..<size) {
                if (priority > list[i].priority ) {
                    list.add(i, handler)
                    return
                }
            }
            list.add(size, handler)
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
                    subscriptions[eventType] = ArrayList(handlerList).apply { remove(handler) }
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
            val queue = postingState.eventQueue
            if (queue == null) {
                postingState.eventQueue = ArrayDeque<Any>().apply { add(event) }
            } else {
                queue.add(event)
            }
            return
        }
        postingState.isPosting = true
        postingCount.incrementAndGet()

        try {
            val isMainThread = Looper.getMainLooper() == Looper.myLooper()
            postSingleEvent(event, isMainThread)
            var deferEvent = postingState.eventQueue?.removeFirstOrNull()
            while (deferEvent != null) {
                postSingleEvent(deferEvent, isMainThread)
                deferEvent = postingState.eventQueue?.removeFirstOrNull()
            }
        } finally {
            postingState.isPosting = false
            postingCount.decrementAndGet()
        }
    }

    private fun postSingleEvent(event: Any, isMainThread: Boolean) {
        val eventClass: Class<*> = event::class.java
        if (eventInheritance) {
            val eventTypes: List<Class<*>> = lookupAllEventTypes(eventClass)
            val countTypes = eventTypes.size
            for (h in 0 until countTypes) {
                postEventForEventType(event, isMainThread, eventTypes[h])
            }
        } else {
            postEventForEventType(event, isMainThread, eventClass)
        }
    }

    private fun postEventForEventType(event: Any, isMainThread: Boolean, eventClass: Class<*>) {
        val handlerList = synchronized(this) { subscriptions[eventClass] } ?: return
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

    // 查找当前类型的所有层级的父类，以及接口
    private fun lookupAllEventTypes(eventClass: Class<*>): List<Class<*>> {
        synchronized(eventTypesCache) {
            return eventTypesCache.getOrPut(eventClass) {
                val eventTypes = ArrayList<Class<*>>()
                var clazz: Class<*>? = eventClass
                while (clazz != null) {
                    eventTypes.add(clazz)
                    addInterfaces(eventTypes, clazz.getInterfaces())
                    clazz = clazz.superclass
                }
                ArrayList(eventTypes)
            }
        }
    }

    private fun addInterfaces(eventTypes: MutableList<Class<*>>, interfaces: Array<Class<*>>) {
        for (interfaceClass in interfaces) {
            if (!eventTypes.contains(interfaceClass)) {
                eventTypes.add(interfaceClass)
                addInterfaces(eventTypes, interfaceClass.getInterfaces())
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

    /**
     * 设置是否启用事件继承
     */
    fun setEventInheritance(enable: Boolean) {
        eventInheritance = enable
    }
}
