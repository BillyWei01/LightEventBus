package io.github.lightevent.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.lightevent.EventBus
import io.github.lightevent.EventHandler
import io.github.lightevent.ThreadMode
import io.github.lightevent.event.NormalEvent
import io.github.lightevent.event.RemoveStickyEvent
import io.github.lightevent.event.StickyEvent
import io.github.lightevent.util.Utils

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {
    private object RecursiveEvent
    private object SubRecursiveEvent


    private val handlers: List<EventHandler<*>> by lazy {
        val eventTv = findViewById<TextView>(R.id.eventTv)
        listOf(
            EventHandler.create<NormalEvent>(threadMode = ThreadMode.MAIN) {
                eventTv.text = "${it::class.simpleName}: ${it.time}"
            },
            EventHandler.create<StickyEvent>(threadMode = ThreadMode.MAIN) {
                eventTv.text = "${it::class.simpleName}: ${it.time}"
            },
            EventHandler.create<RemoveStickyEvent>(threadMode = ThreadMode.MAIN) {
                eventTv.text = "Sticky event had been removed"
            },
            EventHandler.create<RecursiveEvent>(threadMode = ThreadMode.POSTING) {
                EventBus.getDefault().post(SubRecursiveEvent)
            },
            EventHandler.create<SubRecursiveEvent>(threadMode = ThreadMode.MAIN){
                eventTv.text = "Get event: ${it::class.simpleName}"
            },
        )
    }

    private fun onNormalEvent(event: NormalEvent){
        Log.d("TAG", "event:${event::class.simpleName}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 测试常规事件
        findViewById<Button>(R.id.postEventBtn).setOnClickListener {
            val time = Utils.timestampToDate(System.currentTimeMillis())
            EventBus.getDefault().post(NormalEvent(time))
        }

        // 测试粘性事件
        findViewById<Button>(R.id.testStickyBtn).setOnClickListener {
            startActivity(Intent(this, StickyTestActivity::class.java))
        }

        // 测试线程模式
        findViewById<Button>(R.id.testThreadModeBtn).setOnClickListener {
            startActivity(Intent(this, ThreadModeTestActivity::class.java))
        }

        // 测试递归事件
        findViewById<Button>(R.id.testRecursiveBtn).setOnClickListener {
            EventBus.getDefault().post(RecursiveEvent)
        }

        findViewById<Button>(R.id.benchmarkBtn).setOnClickListener {
            startActivity(Intent(this, BenchmarkActivity::class.java))
        }

        EventBus.getDefault().register(handlers)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(handlers)
    }
}