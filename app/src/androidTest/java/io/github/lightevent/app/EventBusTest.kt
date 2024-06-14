package io.github.lightevent.app

import io.github.lightevent.EventBus
import io.github.lightevent.EventHandler
import org.junit.Assert
import org.junit.Test

class EventBusTest {
    /**
     * 测试优先级的降序
     *
     * 优先级不同，则优先级越高，越先被处理
     */
    @Test
    fun testDownOrderPriority() {
        val eventBus = EventBus.get("testDownOrderPriority")

        val eventList = mutableListOf<Int>()

        val handler1 = listOf(
            EventHandler.create<String>(priority = 1) {
                eventList.add(1)
            }
        )
        val handler2 = listOf(
            EventHandler.create<String>(priority = 2) {
                eventList.add(2)
            }
        )
        val handler3 = listOf(
            EventHandler.create<String>(priority = 3) {
                eventList.add(3)
            }
        )

        eventBus.register(handler1)
        eventBus.register(handler3)
        eventBus.register(handler2)


        eventBus.post("hello")

        eventBus.unregister(handler1)
        eventBus.unregister(handler3)
        eventBus.unregister(handler2)

        Assert.assertEquals(3, eventList[0])
        Assert.assertEquals(2, eventList[1])
        Assert.assertEquals(1, eventList[2])
    }

    /**
     * 测试优先级相等
     *
     * 优先级相同，先订阅者，先被处理
     */
    @Test
    fun testEqualOrderPriority() {
        val eventBus = EventBus.get("testEqualOrderPriority")

        val eventList = mutableListOf<Int>()

        val handler1 = listOf(
            EventHandler.create<String> {
                eventList.add(1)
            }
        )
        val handler2 = listOf(
            EventHandler.create<String> {
                eventList.add(2)
            }
        )
        val handler3 = listOf(
            EventHandler.create<String> {
                eventList.add(3)
            }
        )

        eventBus.register(handler1)
        eventBus.register(handler3)
        eventBus.register(handler2)


        eventBus.post("hello")

        eventBus.unregister(handler1)
        eventBus.unregister(handler3)
        eventBus.unregister(handler2)

        Assert.assertEquals(1, eventList[0])
        Assert.assertEquals(3, eventList[1])
        Assert.assertEquals(2, eventList[2])
    }

    private open class Parent
    private class Son: Parent()


    @Test
    fun testEventInheritance() {
        val eventBus = EventBus.get("testEventInheritance")

        val eventList = mutableListOf<Class<*>>()

        val handler = listOf(
            EventHandler.create<Parent> {
                eventList.add(Parent::class.java)
            },
            EventHandler.create<Son> {
                eventList.add(Son::class.java)
            }
        )

        eventBus.register(handler)

        eventBus.post(Parent())
        Assert.assertEquals(1, eventList.size)
        Assert.assertEquals(Parent::class.java, eventList[0])

        eventList.clear()

        eventBus.post(Son())
        Assert.assertEquals(2, eventList.size)
        // 先发送给最具体的类型（子类）的订阅方法，再发送父类的订阅方法
        Assert.assertEquals(Son::class.java, eventList[0])
        Assert.assertEquals(Parent::class.java, eventList[1])

        eventBus.unregister(handler)
    }

    @Test
    fun testDisableEventInheritance(){
        val eventBus = EventBus.get("testDisableEventInheritance")

        val eventList = mutableListOf<Class<*>>()

        val handler = listOf(
            EventHandler.create<Parent> {
                eventList.add(Parent::class.java)
            },
            EventHandler.create<Son> {
                eventList.add(Son::class.java)
            }
        )

        eventBus.register(handler)

        eventBus.post(Parent(), true)
        Assert.assertEquals(1, eventList.size)
        Assert.assertEquals(Parent::class.java, eventList[0])

        eventList.clear()

        eventBus.post(Son(), true)
        Assert.assertEquals(1, eventList.size)
        // 先发送给最具体的类型（子类）的订阅方法，再发送父类的订阅方法
        Assert.assertEquals(Son::class.java, eventList[0])

        eventBus.unregister(handler)
    }
}