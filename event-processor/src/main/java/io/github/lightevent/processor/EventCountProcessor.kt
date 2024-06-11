package io.github.lightevent.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import java.io.File

class EventCountProcessor(env: SymbolProcessorEnvironment) : SymbolProcessor {
    private val eventCount: Int
    private val generatedPath: String
    private val codeGenerator: CodeGenerator
    private val logger: KSPLogger

    companion object {
        private const val PACKAGE_NAME = "io.github.lightevent.benchmark"
    }

    init {
        val options: Map<String, String> = env.options
        eventCount = options["eventCount"]?.toIntOrNull() ?: 10
        generatedPath = options["generatedPath"] ?: ""

        codeGenerator = env.codeGenerator
        logger = env.logger

        logger.warn("init, eventCount:$eventCount, generatedPath:$generatedPath")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        generateTestEvents()
        generateIndexEventBusTest()
        generateReflectionEventBusTest()
        generateLightEventTest()
        generateRxBusTest()
        generateLiveEventBusTest()
        return emptyList()
    }

    private fun generateTestEvents() {
        val fileName = "TestEvents"
        if(!needToGenerate(fileName)) return

        val builder = StringBuilder()
        builder.append("package").append(' ').append(PACKAGE_NAME).append("\n\n")
        for (i in 1..eventCount) {
            builder.append("class Event").append(i).append('\n')
        }
        generateFile(fileName, builder.toString())
    }

    private fun generateLightEventTest() {
        val fileName = "LightEventTest"
        if(!needToGenerate(fileName)) return

        val builder = StringBuilder()
        builder.append("package").append(' ').append(PACKAGE_NAME).append("\n\n")
            .append("import io.github.lightevent.EventBus\n")
            .append("import io.github.lightevent.EventHandler\n\n")
            .append("object $fileName {\n")
            .append("    fun test() : String {\n")

        builder.append("\n        val t0 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        val handler$i = listOf(EventHandler.create<Event$i> {  })\n")
        }
        builder.append("\n        val t1 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        EventBus.getDefault().register(handler$i)\n")
        }
        builder.append("\n        val t2 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        EventBus.getDefault().post(Event$i())\n")
        }
        builder.append("\n        val t3 = System.nanoTime()\n\n")

        for (i in 1..eventCount) {
            builder.append("        EventBus.getDefault().unregister(handler$i)\n")
        }

        builder.append("\n        val t4 = System.nanoTime()\n\n")

        writeTime(builder)

        generateFile(fileName, builder.toString())
    }

    private fun generateReflectionEventBusTest() {
        val fileName = "ReflectionEventBusTest"
        if (!needToGenerate(fileName)) return

        val builder = StringBuilder()
        builder.append("package").append(' ').append(PACKAGE_NAME).append("\n")
            .append("\n")
            .append("import io.github.lightevent.benchmark.AppEventBusIndex\n")
            .append("import org.greenrobot.eventbus.EventBus\n")
            .append("import org.greenrobot.eventbus.Subscribe\n")
            .append("import org.greenrobot.eventbus.ThreadMode\n")
            .append("\n")
            .append("object $fileName {\n")
            .append("    fun test(): String {")

        builder.append("\n        val t0 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        val handler$i = ReflectionEvent${i}Handler()\n")
        }
        builder.append("\n        val t1 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        EventBus.getDefault().register(handler$i)\n")
        }
        builder.append("\n        val t2 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        EventBus.getDefault().post(Event$i())\n")
        }
        builder.append("\n        val t3 = System.nanoTime()\n\n")

        for (i in 1..eventCount) {
            builder.append("        EventBus.getDefault().unregister(handler$i)\n")
        }

        builder.append("\n        val t4 = System.nanoTime()\n\n")

        writeTime(builder)

        builder.append('\n')

        for (i in 1..eventCount) {
            builder
                .append("class ReflectionEvent${i}Handler {\n")
                .append("    @Subscribe(threadMode = ThreadMode.POSTING)\n")
                .append("    fun onEvent$i(event$i: Event$i) {\n")
                .append("    }\n")
                .append("}\n\n")
        }

        generateFile(fileName, builder.toString())
    }

    private fun generateIndexEventBusTest() {
        val fileName = "IndexEventBusTest"
        if (!needToGenerate(fileName)) return

        val builder = StringBuilder()
        builder.append("package").append(' ').append(PACKAGE_NAME).append("\n")
            .append("\n")
            .append("import io.github.lightevent.benchmark.AppEventBusIndex\n")
            .append("import org.greenrobot.eventbus.EventBus\n")
            .append("import org.greenrobot.eventbus.Subscribe\n")
            .append("import org.greenrobot.eventbus.ThreadMode\n")
            .append("\n")
            .append("object $fileName {\n")
            .append("    private val _eventBus by lazy {\n")
            .append("        EventBus.builder().addIndex(AppEventBusIndex()).build()\n")
            .append("    }\n")
            .append("\n")
            .append("    fun test(): String {")

        builder.append("\n        val t0 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        val handler$i = IndexEvent${i}Handler()\n")
        }
        builder.append("\n        val eventBus = _eventBus\n")
        builder.append("\n        val t1 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        eventBus.register(handler$i)\n")
        }
        builder.append("\n        val t2 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        eventBus.post(Event$i())\n")
        }
        builder.append("\n        val t3 = System.nanoTime()\n\n")

        for (i in 1..eventCount) {
            builder.append("        eventBus.unregister(handler$i)\n")
        }

        builder.append("\n        val t4 = System.nanoTime()\n\n")

        writeTime(builder)

        builder.append("\n")

        for (i in 1..eventCount) {
            builder
                .append("class IndexEvent${i}Handler {\n")
                .append("    @Subscribe(threadMode = ThreadMode.POSTING)\n")
                .append("    fun onEvent$i(event$i: Event$i) {\n")
                .append("    }\n")
                .append("}\n\n")
        }

        generateFile(fileName, builder.toString())
    }

    private fun generateRxBusTest() {
        val fileName = "RxBusTest"
        if(!needToGenerate(fileName)) return

        val builder = StringBuilder()
        builder.append("package").append(' ').append(PACKAGE_NAME).append("\n\n")
            .append("import io.reactivex.rxjava3.functions.Consumer\n")
            .append("import io.reactivex.rxjava3.subjects.PublishSubject\n\n")
            .append("object $fileName {\n")
            .append("    fun test() : String {\n")

        builder.append("\n        val t0 = System.nanoTime()\n\n")
        builder.append("        val bus = PublishSubject.create<Any>().toSerialized()\n")
        builder.append("\n        val t1 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        val disposable$i = bus.ofType(Event$i::class.java).subscribe(Consumer {  })\n")
        }
        builder.append("\n        val t2 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        bus.onNext(Event$i())\n")
        }
        builder.append("\n        val t3 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        disposable$i.dispose()\n")
        }
        builder.append("\n        val t4 = System.nanoTime()\n\n")

        writeTime(builder)

        generateFile(fileName, builder.toString())
    }


    private fun generateLiveEventBusTest() {
        val fileName = "LiveEventBusTest"
        if(!needToGenerate(fileName)) return

        val builder = StringBuilder()
        builder.append("package").append(' ').append(PACKAGE_NAME).append("\n\n")
            .append("import androidx.lifecycle.LifecycleOwner\n")
            .append("import androidx.lifecycle.Observer\n")
            .append("import com.jeremyliao.liveeventbus.LiveEventBus\n\n")
            .append("object $fileName {\n")
            .append("    fun test(lifecycleOwner: LifecycleOwner) : String {\n")

        builder.append("\n        val t0 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        val observer$i = Observer<Event$i> {  }\n")
        }
        builder.append("\n        val t1 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        LiveEventBus.get<Event$i>(\"Event$i\").observe(lifecycleOwner, observer$i)\n")
        }
        builder.append("\n        val t2 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        LiveEventBus.get<Event$i>(\"Event$i\").post(Event$i())\n")
        }
        builder.append("\n        val t3 = System.nanoTime()\n\n")
        for (i in 1..eventCount) {
            builder.append("        LiveEventBus.get<Event$i>(\"Event$i\").removeObserver(observer$i)\n")
        }
        builder.append("\n        val t4 = System.nanoTime()\n\n")

        writeTime(builder)

        generateFile(fileName, builder.toString())
    }

    private fun writeTime(builder: StringBuilder) {
        builder
            .append(
                "        return \"prepare:\${formatTime(t1 - t0)}, " +
                        "register:\${formatTime(t2 - t1)}, " +
                        "post:\${formatTime(t3 - t2)}, " +
                        "unregister:\${formatTime(t4 - t3)}\"\n"
            )
            .append("    }\n\n")
            .append("    private fun formatTime(time: Long): String {\n")
            .append("        return \"\${time / 1000_000}.\${(time % 1000_000) / 10000}\"\n")
            .append("    }\n")
            .append('}')
            .append('\n')
    }


    private fun needToGenerate(fileName: String): Boolean {
        val parent = File(generatedPath + PACKAGE_NAME.replace('.', File.separatorChar))
        val targetFile = File(parent, "${fileName}.kt")
        if (targetFile.exists()) {
            val text = targetFile.readText()
            return if (text.contains("Event$eventCount") &&
                !text.contains("Event${eventCount + 1}")
            ) {
                logger.warn("needToGenerate, $targetFile no changes")
                false
            } else {
                targetFile.delete()
                logger.warn("needToGenerate, $targetFile regenerate")
                true
            }
        } else {
            logger.warn("needToGenerate, $targetFile not exist")
        }
        return true
    }


    private fun generateFile(fileName: String, content: String) {
        val outputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false),
            packageName = PACKAGE_NAME,
            fileName = fileName
        )

        outputStream.use {
            it.bufferedWriter().use { writer ->
                writer.write(content)
            }
        }
    }
}