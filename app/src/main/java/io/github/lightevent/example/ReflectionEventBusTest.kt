package io.github.lightevent.example

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

object ReflectionEventBusTest {
    fun test(): String {
        val t0 = System.nanoTime()

        val handler1 = ReflectionEvent1Handler()

        val t1 = System.nanoTime()

        EventBus.getDefault().register(handler1)

        val t2 = System.nanoTime()

        EventBus.getDefault().post(Event1())

        val t3 = System.nanoTime()

        EventBus.getDefault().unregister(handler1)

        val t4 = System.nanoTime()

        return "instantiate:${formatTime(t1 - t0)}, register:${formatTime(t2 - t1)}, " +
                "post${formatTime(t3 - t2)}, unregister:${formatTime(t4 - t3)}"
    }

    private fun formatTime(time: Long): String{
        return "${time / 1000_000}.${(time % 1000_000) / 100000}"
    }
}

class ReflectionEvent1Handler {
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent1(event: Event1) {
    }
}