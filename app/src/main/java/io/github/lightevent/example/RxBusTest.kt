package io.github.lightevent.example

import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.subjects.PublishSubject

object RxBusTest {
    fun test() : String{
        val t0 = System.nanoTime()

        val bus = PublishSubject.create<Any>().toSerialized()

        val t1 = System.nanoTime()

        val disposable1 = bus.ofType(Event1::class.java).subscribe(Consumer {  })

        val t2 = System.nanoTime()

        bus.onNext(Event1())

        val t3 = System.nanoTime()

        disposable1.dispose()

        val t4 = System.nanoTime()

        return "instantiate:${formatTime(t1 - t0)}, register:${formatTime(t2 - t1)}, " +
                "post:${formatTime(t3 - t2)}, unregister:${formatTime(t4 - t3)}"
    }

    private fun formatTime(time: Long): String{
        return "${time / 1000_000}.${(time % 1000_000) / 10000}"
    }
}