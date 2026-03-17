import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class OutputResourcesScannerTest {
    @Test
    fun scanCollectsDrawableRawAndStringIds() {
        val root = createTempDirectory("resource-scan").toFile()
        val prefix = File(root, "SampleRes").apply { mkdirs() }
        File(prefix, "drawable").mkdirs()
        File(prefix, "raw").mkdirs()
        File(prefix, "en.lproj").mkdirs()
        File(prefix, "drawable/bg_card.9.png").writeText("png")
        File(prefix, "raw/demo_file.json").writeText("{}")
        File(prefix, "en.lproj/Localizable.strings").writeText("\"app_name\" = \"Demo\";\n")

        val available = OutputResourcesScanner().scan(root)

        assertEquals(setOf("bg_card"), available.drawable)
        assertEquals(setOf("demo_file"), available.raw)
        assertEquals(setOf("app_name"), available.strings)
    }
}
