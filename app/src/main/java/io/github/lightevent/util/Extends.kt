package io.github.lightevent.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.github.lightevent.EventBus
import io.github.lightevent.EventHandler

fun EventBus.registerEventHandlers(
    lifecycleOwner: LifecycleOwner,
    handlers: List<EventHandler<*>>,
    unregisterEvent: Lifecycle.Event = Lifecycle.Event.ON_DESTROY
) {
    lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == unregisterEvent) {
                unregister(handlers)
                source.lifecycle.removeObserver(this)
            }
        }
    })
    register(handlers)
}