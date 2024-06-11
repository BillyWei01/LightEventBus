package io.github.lightevent.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.lightevent.benchmark.Benchmark
import java.util.concurrent.Executors

class BenchmarkActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_benchmark)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.benchmark)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.startBtn).setOnClickListener {
            executor.execute {
                val result = Benchmark.start(this)
                it.post {
                    findViewById<TextView>(R.id.resultTv).text = result
                }
            }
        }
    }
}
