# LightEventBus

[![Maven Central](https://img.shields.io/maven-central/v/io.github.billywei01/lightevent)](https://search.maven.org/artifact/io.github.billywei01/lightevent)

## 一、概述
LightEventBus是基于Android平台的轻量级的事件总线。<br>

LightEventBus的功能类似于 [EventBus](https://github.com/greenrobot/EventBus)：支持线程模式、粘性事件、优先级等功能。<br>

除了“轻量”的特点之外，LightEventBus 在性能方面，也有一定优化。

| 方式                     | 准备 | 注册  | 发送  | 取消注册 |
| ----------------------- | ---- | --- | --- | ---- |
| **Index-EventBus**      | 14.9 | 4.1 | 3.1 | 0.4  |
| **Reflection-EventBus** | 0.8  | 8.7 | 1.6 | 0.4  |
| **LiveEventBus**        | 0.6  | 7.1 | 1.1 | 1.7  |
| **RxBus**               | 17.4 | 3.5 | 4.4 | 0.3  |
| **LightEventBus**       | 0.6  | 0.4 | 1.0 | 0.2  |

<br>

## 二、用法

### 2.1 代码引入

```gradle
implementation("io.github.billywei01:lightevent:1.0.7")
```

### 2.2 声明事件
```kotlin
data class NormalEvent(val time: String)
```

### 2.3 事件处理
使用 LightEventBus，订阅方法不需要声明为类的方法，不需要添加注解。 <br>
只需要定义一个`EventHandler` 实例（包含方法和参数）。

`EventHandler`的声明如下：

```kotlin
// 响应事件的方法
typealias Action<T> = (event: T) -> Unit

// 事件Handler
class EventHandler<T>( 
    val eventType: Class<*>,
    val sticky: Boolean,
    val threadMode: ThreadMode,
    val action: Action<T>  
) {
    companion object {
        // 增加一个静态方法，方便构建实例
        inline fun <reified T> create(
            sticky: Boolean = false,
            threadMode: ThreadMode = ThreadMode.POSTING,
            noinline action: Action<T>
        ): EventHandler<T> {
            return EventHandler(T::class.java, sticky, threadMode, action)
        }
    }
}
```

备注：`EventHandler`是源码的一部分，使用时不需要定义。<br>
<br>

`EventHandler`有多种创建方式，可以用直接`new`，也可以同通过`create`方法创建（可以少传一些参数）。 <br>

例如：
```kotlin
val handler = EventHandler.create<NormalEvent>(threadMode = ThreadMode.MAIN) { event ->
    Log.d("TAG", "event:${event::class.simpleName}")
}
```

`EventHandler` 将 `action` 放在最后一个参数，故而可以用如上的 lambda 形式传参。 <br>

<br>
如果已经声明的方法，可以直接传给`create`方法：

```kotlin
private fun onNormalEvent(event: NormalEvent){
    Log.d("TAG", "event:${event::class.simpleName}")
}
EventHandler.create(action = ::onNormalEvent)
```

<br>

`EventHandler` 支持设定线程模式、粘性事件和优先级：

```kotlin
EventHandler.create(threadMode = ThreadMode.ASYNC, sticky = true, priority = 100, action = ::onNormalEvent)
```

<br>

### 2.4 订阅/取消订阅
```kotlin
private val handlers: List<EventHandler<*>> by lazy {
    listOf(
        EventHandler.create<NormalEvent> { event ->
            Log.d("TAG", "event:${event::class.simpleName}")
        }
    )
}

EventBus.getDefault().register(handlers)

EventBus.getDefault().unregister(handlers)    
```

订阅事件的用法和EventBus有些区别：<br>
原版 EventBus 传入的是订阅者（订阅方法所在的类），<br>
而 LightEventBus 传入的是方法列表。

<br>

如果运行环境有 `LifecycleOwner`，可自定义扩展函数，在特定的生命周期取消订阅。<br>


```kotlin
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
```

<br>

### 2.5发布事件
```kotlin
// 发布事件
EventBus.getDefault().post(NormalEvent(time)) 

// 发布粘性事件
EventBus.getDefault().postSticky(StickyEvent(time))
```

## 三、参考链接
原理方面，可参考文章：<br>
https://juejin.cn/post/7379831020495749157

关于LightEventBus的实现，可参考文章：<br>
https://juejin.cn/spost/7380694342745112614

## License
See the [LICENSE](LICENSE.md) file for license rights and limitations.

