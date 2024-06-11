package io.github.lightevent.benchmark

import android.util.Log
import androidx.lifecycle.LifecycleOwner

/**
 * 基准测试
 *
 * 测试代码由 ksp 生成。
 * 生成的事件数量由 build.gradle.kts 的配置控制。
 */
object Benchmark {
    private const val TAG = "Benchmark"

    fun start(lifecycleOwner: LifecycleOwner): String {
        val indexEventBus = IndexEventBusTest.test()
        val reflectionEventBus = ReflectionEventBusTest.test()
        val rxBus = RxBusTest.test()
        val liveEventBus = LiveEventBusTest.test(lifecycleOwner)
        val lightEvent = LightEventTest.test()

        val message = "IndexEventBus: $indexEventBus\n" +
                "ReflectionEventBus: $reflectionEventBus\n" +
                "RxBus: $rxBus\n" +
                "LiveEventBus: $liveEventBus\n" +
                "LightEventBus: $lightEvent\n"

        Log.i(TAG, "$message ")

        return message
            .replace("\n", "\n\n")
            .replace(": ", ":\n")
    }
}
