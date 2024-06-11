# LightEventBus

LightEventBus是基于Android平台的轻量级的事件总线。

其功能类似于 [EventBus](https://github.com/greenrobot/EventBus), 但API有些差异。<br>


LightEventBus 具体用法如下：

# 用法

## 1、代码引入
现阶段代码没有发布到 maven 中央仓库，引用代码可直接拷贝源码到项目中。<br>

## 2、声明事件
```kotlin
data class NormalEvent(val time: String)
```

## 3、创建事件处理
使用 LightEventBus，订阅方法不需要声明为类的方法，不需要添加注解。 <br>
只需要定义一个`EventHandler` 实例（包含方法和参数）。

`EventHandler`的类定义（LightEventBus的一部分，使用时不需要定义）如下：

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

`EventHandler`有多种创建方式，可以用直接`new`，也可以同通过`create`方法创建（可以少传一些参数）。 <br>
例如：
```kotlin
val handler = EventHandler.create<NormalEvent> { event ->
    Log.d("TAG", "event:${event::class.simpleName}")
}
```

`EventHandler` 将 `action` 放在最后一个参数，故而可以用如上的 lambda 形式传参。

如果已经声明的方法，可以直接传给`create`方法：

```kotlin
private fun onNormalEvent(event: NormalEvent){
    Log.d("TAG", "event:${event::class.simpleName}")
}
EventHandler.create(action = ::onNormalEvent)
```

`EventHandler`支持设定粘性事件和线程模式：

```kotlin
EventHandler.create(sticky = true, threadMode = ThreadMode.ASYNC, action = ::onNormalEvent)
```


## 4、订阅/取消订阅
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

如果运行环境有 `LifecycleOwner`，可自定义扩展函数，在特定的生命周期取消订阅。

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

## 5、发布事件
```kotlin
// 发布事件
EventBus.getDefault().post(NormalEvent(time)) 

// 发布粘性事件
EventBus.getDefault().postSticky(StickyEvent(time))
```

# License
See the [LICENSE](LICENSE.md) file for license rights and limitations.

