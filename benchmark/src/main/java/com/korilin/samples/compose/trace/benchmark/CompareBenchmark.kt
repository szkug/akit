package cn.szkug.samples.compose.trace.benchmark

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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class CompareBenchmark : AbstractComposeBenchmark(StartupMode.WARM) {

    @Test
    fun benchmarkCompareScreen() = benchmark()

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    override val metrics: List<Metric> =
        listOf(
            FrameTimingMetric(),
            TraceSectionMetric("GlideImage", TraceSectionMetric.Mode.Average),
            TraceSectionMetric("AsyncImage", TraceSectionMetric.Mode.Average),
        )

    override fun MacrobenchmarkScope.setupBlock() {
    }

    override fun MacrobenchmarkScope.measureBlock() {
        startActivityAndWait()
        val selector = By.text("Compare Glide and Coil")
        device.wait(Until.hasObject(selector), 5_500)
        val launch = device.findObject(selector)
        launch.click()

        val back = By.text("Back")
        device.wait(Until.hasObject(back), 5_500)
        device.findObject(back).click()
    }

}
