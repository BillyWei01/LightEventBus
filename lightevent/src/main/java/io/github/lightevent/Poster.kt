package io.github.lightevent

import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * 异步消息 Poster
 *
 * @param concurrentSize 并发大小
 */
internal class Poster(concurrentSize: Int) {
    companion object {
        private var realExecutor: Executor? = null

        fun setExecutor(executor: Executor) {
            realExecutor = executor
        }

        private fun getExecutor(): Executor {
            val executor = realExecutor
            if (executor != null) return executor
            return Executors.newCachedThreadPool().apply {
                realExecutor = this
            }
        }
    }

    private var count = 0
    private val windowSize: Int = if (concurrentSize > 0) concurrentSize else 1
    private val queue = ArrayDeque<Runnable>()

    private val executor: Executor
        get() = getExecutor()

    private class Task(val action: Action<Any>, val event: Any) : Runnable {
        override fun run() {
            action(event)
        }
    }

    @Synchronized
    fun enqueue(action: Action<Any>, event: Any) {
        val task = Task(action, event)
        if (count < windowSize) {
            start(task)
        } else {
            queue.addLast(task)
        }
    }

    private fun start(r: Runnable) {
        count++
        executor.execute {
            try {
                r.run()
            } finally {
                scheduleNext()
            }
        }
    }

    @Synchronized
    private fun scheduleNext() {
        count--
        queue.removeFirstOrNull()?.let { next ->
            start(next)
        }
    }
}