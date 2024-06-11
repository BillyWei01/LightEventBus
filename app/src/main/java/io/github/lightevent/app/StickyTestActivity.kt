package io.github.lightevent.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.lightevent.util.Utils
import io.github.lightevent.EventBus
import io.github.lightevent.EventHandler
import io.github.lightevent.event.RemoveStickyEvent
import io.github.lightevent.event.StickyEvent
import io.github.lightevent.util.registerEventHandlers

@SuppressLint("SetTextI18n")
class StickyTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sticky_test)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sticky)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val stickEventTv = findViewById<TextView>(R.id.stickEventTv)

        findViewById<Button>(R.id.postStickyEventBtn).setOnClickListener {
            val time = Utils.timestampToDate(System.currentTimeMillis())
            EventBus.getDefault().postSticky(StickyEvent(time))
        }

        findViewById<Button>(R.id.removeStickyEventBtn).setOnClickListener {
            if (EventBus.getDefault().hasStickyEvent(StickyEvent::class.java)) {
                EventBus.getDefault().removeStickyEvent(StickyEvent::class.java)
                EventBus.getDefault().post(RemoveStickyEvent)
                stickEventTv.text = "Sticky Event had been removed"
            } else {
                stickEventTv.text = "No Sticky Event"
            }
        }

       EventBus.getDefault().registerEventHandlers(
           this,
            listOf(
                EventHandler.create<StickyEvent>(sticky = true) {
                    stickEventTv.text = "${it::class.simpleName}: ${it.time}"
                }
            )
        )
    }
}