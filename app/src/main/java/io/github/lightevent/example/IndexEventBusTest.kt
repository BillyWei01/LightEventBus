package io.github.lightevent.example

import io.github.lightevent.benchmark.AppEventBusIndex
import io.github.lightevent.benchmark.IndexEventBusTest
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

object IndexEventBusTest {
    private val _eventBus by lazy {
        EventBus.builder().addIndex(AppEventBusIndex()).build()
    }

    fun test(): String {
        val t0 = System.nanoTime()

        val handler1 = IndexEvent1Handler()

        val eventBus = _eventBus

        val t1 = System.nanoTime()

        eventBus.register(handler1)

        val t2 = System.nanoTime()

        eventBus.post(Event1())

        val t3 = System.nanoTime()

        eventBus.unregister(handler1)

        val t4 = System.nanoTime()

        return "instantiate:${formatTime(t1 - t0)}, register:${formatTime(t2 - t1)}, " +
                "post${formatTime(t3 - t2)}, unregister:${formatTime(t4 - t3)}"
    }

    private fun formatTime(time: Long): String{
        return "${time / 1000_000}.${(time % 1000_000) / 10000}"
    }
}

class IndexEvent1Handler {
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent1(event: Event1) {
    }
}
