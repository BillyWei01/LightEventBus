package io.github.lightevent.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.lightevent.EventBus
import io.github.lightevent.EventHandler
import io.github.lightevent.ThreadMode
import io.github.lightevent.event.PostThread
import io.github.lightevent.util.Utils
import io.github.lightevent.util.registerEventHandlers

open class ThreadEvent {
    private val time: String = Utils.timestampToDate(System.currentTimeMillis())
    var postThreadName: String = ""

    override fun toString(): String {
        return "event: ${this::class.simpleName}\n" +
                "time: ${time}\n" +
                "postThread: $postThreadName\n" +
                "eventThread: ${Thread.currentThread().name} "
    }
}

class PostingEvent : ThreadEvent()
class MainEvent : ThreadEvent()
class MainOrderEvent : ThreadEvent()
class BackgroundEvent : ThreadEvent()
class AsyncEvent : ThreadEvent()

class ThreadModeTestActivity : AppCompatActivity() {
    private var postThread = PostThread.MAIN

    // 测试用独立的通道
    private val eventBus = EventBus.get("ThreadModeTest")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_thread_mode_test)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.threaMode)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val radioMain = findViewById<Button>(R.id.radioMain)
        val radioBackground = findViewById<Button>(R.id.radioBackground)

        val postingBtn = findViewById<Button>(R.id.postingBtn)
        val mainBtn = findViewById<Button>(R.id.mainBtn)
        val mainOrderBtn = findViewById<Button>(R.id.mainOrderBtn)
        val backgroundBtn = findViewById<Button>(R.id.backgroundBtn)
        val asyncBtn = findViewById<Button>(R.id.asyncBtn)

        radioMain.setOnClickListener {
            postThread = PostThread.MAIN
        }

        radioBackground.setOnClickListener {
            postThread = PostThread.BACKGROUND
        }

        postingBtn.setOnClickListener {
            postEvent(PostingEvent())
        }

        mainBtn.setOnClickListener {
            postEvent(MainEvent())
        }

        mainOrderBtn.setOnClickListener {
            postEvent(MainOrderEvent())
        }

        backgroundBtn.setOnClickListener {
            postEvent(BackgroundEvent())
        }

        asyncBtn.setOnClickListener {
            postEvent(AsyncEvent())
        }

        eventBus.registerEventHandlers(
            this,
            listOf(
                EventHandler.create<PostingEvent>(threadMode = ThreadMode.POSTING) { event ->
                    updateText(event.toString())
                },
                EventHandler.create<MainEvent>(threadMode = ThreadMode.MAIN) { event ->
                    updateText(event.toString())
                },
                EventHandler.create<MainOrderEvent>(threadMode = ThreadMode.MAIN_ORDERED) { event ->
                    updateText(event.toString())
                },
                EventHandler.create<BackgroundEvent>(threadMode = ThreadMode.BACKGROUND) { event ->
                    updateText(event.toString())
                },
                EventHandler.create<AsyncEvent>(threadMode = ThreadMode.ASYNC) { event ->
                    updateText(event.toString())
                }
            )
        )
    }

    private fun postEvent(event: ThreadEvent) {

        if (postThread == PostThread.MAIN) {
            event.postThreadName = Thread.currentThread().name
            eventBus.post(event)
        } else {
            Thread(
                {
                    event.postThreadName = Thread.currentThread().name
                    eventBus.post(event)
                },
                "background"
            ).start()
        }
    }

    private fun updateText(text: String) {
        findViewById<TextView>(R.id.threadModeTv)?.let {
            it.post { it.text = text }
        }
    }
}
