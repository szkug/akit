package cn.szkug.samples.compose.trace.benchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class RoomGridListBenchmark : AbstractComposeBenchmark(StartupMode.COLD) {

    @Test
    fun benchmarkRoomGridListScreen() = benchmark()

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    override val metrics: List<Metric> =
        listOf(
            FrameTimingMetric(),
            TraceSectionMetric("RoomGridList", TraceSectionMetric.Mode.Sum),
            TraceSectionMetric("RoomGridItem", TraceSectionMetric.Mode.Sum),
            TraceSectionMetric("RoomTagsRow", TraceSectionMetric.Mode.Sum),
            TraceSectionMetric("RoomTag", TraceSectionMetric.Mode.Sum),
        )

    override fun MacrobenchmarkScope.setupBlock() {
    }

    override fun MacrobenchmarkScope.measureBlock() {
        startActivityAndWait()
        val selector = By.text("RoomGridList")
        device.wait(Until.hasObject(selector), 5_500)
        val launch = device.findObject(selector)
        launch.click()
        device.wait(
            Until.hasObject(By.clazz("$packageName.RoomGridListActivity")),
            TimeUnit.SECONDS.toMillis(5)
        )
    }

}
