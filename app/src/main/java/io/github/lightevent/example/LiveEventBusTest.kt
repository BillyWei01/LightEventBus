package io.github.lightevent.example

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus

object LiveEventBusTest {
    fun test(lifecycleOwner: LifecycleOwner) : String{
        val t0 = System.nanoTime()

        val observer1 = Observer<Event1> {  }

        val t1 = System.nanoTime()

        LiveEventBus.get<Event1>("Event1").observe(lifecycleOwner, observer1)

        val t2 = System.nanoTime()

        LiveEventBus.get<Event1>("Event1").post(Event1())

        val t3 = System.nanoTime()

        LiveEventBus.get<Event1>("Event1").removeObserver(observer1)

        val t4 = System.nanoTime()

        return "instantiate:${formatTime(t1 - t0)}, register:${formatTime(t2 - t1)}, " +
                "post:${formatTime(t3 - t2)}, unregister:${formatTime(t4 - t3)}"
    }

    private fun formatTime(time: Long): String{
        return "${time / 1000_000}.${(time % 1000_000) / 10000}"
    }
}