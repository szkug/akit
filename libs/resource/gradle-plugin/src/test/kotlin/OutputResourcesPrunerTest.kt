import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutputResourcesPrunerTest {
    @Test
    fun pruneRemovesUnusedDrawableAndStringEntries() {
        val root = createTempDirectory("resource-prune").toFile()
        val prefix = File(root, "SampleRes").apply { mkdirs() }
        File(prefix, "drawable").mkdirs()
        File(prefix, "en.lproj").mkdirs()
        val keepDrawable = File(prefix, "drawable/keep.png").apply { writeText("png") }
        val dropDrawable = File(prefix, "drawable/drop.png").apply { writeText("png") }
        val strings = File(prefix, "en.lproj/Localizable.strings").apply {
            writeText("\"keep\" = \"Keep\";\n\"drop\" = \"Drop\";\n")
        }

        OutputResourcesPruner().prune(
            root = root,
            used = UsedResources(
                drawable = setOf("keep"),
                raw = emptySet(),
                strings = setOf("keep"),
                drawableCalls = emptyMap(),
                rawCalls = emptyMap(),
                stringCalls = emptyMap(),
            ),
        )

        assertTrue(keepDrawable.exists())
        assertFalse(dropDrawable.exists())
        val content = strings.readText()
        assertTrue(content.contains("keep"))
        assertFalse(content.contains("drop"))
    }
}
