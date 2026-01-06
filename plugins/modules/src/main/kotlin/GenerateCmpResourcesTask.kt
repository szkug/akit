import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal data class StringResource(
    val name: String,
    val value: String,
)

internal data class DrawableResource(
    val id: String,
    val fileNameWithoutExtension: String,
    val extension: String,
    val relativeDir: String,
)

abstract class GenerateCmpResourcesTask : DefaultTask() {

    @get:InputDirectory
    abstract val resDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val androidNamespace: Property<String>

    @get:Input
    abstract val iosResourcesPrefix: Property<String>

    @TaskAction
    fun generate() {
        val resRoot = resDir.get().asFile
        val strings = parseStrings(resRoot.resolve("values/strings.xml"))
        val drawables = parseDrawables(resRoot)

        val outputRoot = outputDir.get().asFile
        if (outputRoot.exists()) {
            outputRoot.deleteRecursively()
        }
        val commonDir = outputRoot.resolve("commonMain")
        val androidDir = outputRoot.resolve("androidMain")
        val iosDir = outputRoot.resolve("iosMain")
        commonDir.mkdirs()
        androidDir.mkdirs()
        iosDir.mkdirs()

        val pkg = packageName.get()
        val androidPkg = androidNamespace.get().ifBlank { pkg }
        val iosPrefix = iosResourcesPrefix.get()

        writeCommonRes(commonDir, pkg, strings, drawables)
        writeAndroidRes(androidDir, pkg, androidPkg, strings, drawables)
        writeIosRes(iosDir, pkg, iosPrefix, strings, drawables)
    }

    private fun parseStrings(stringsFile: File): List<StringResource> {
        if (!stringsFile.exists()) return emptyList()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile)
        val nodes = doc.getElementsByTagName("string")
        val out = mutableListOf<StringResource>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            val value = node.textContent ?: ""
            out += StringResource(name, value)
        }
        return out.sortedBy { it.name }
    }

    private fun parseDrawables(resRoot: File): List<DrawableResource> {
        if (!resRoot.exists()) return emptyList()
        val drawableDirs = resRoot.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("drawable") }

        val allowedExtensions = setOf("png", "jpg", "jpeg", "webp", "gif")
        val seen = LinkedHashMap<String, DrawableResource>()
        for (dir in drawableDirs) {
            val files = dir.listFiles().orEmpty().filter { it.isFile }
            for (file in files) {
                val extension = file.extension.lowercase()
                if (extension !in allowedExtensions) continue

                val baseName = file.nameWithoutExtension
                val normalizedBase = if (baseName.endsWith(".9")) {
                    baseName.removeSuffix(".9")
                } else {
                    baseName
                }
                val id = sanitizeIdentifier(normalizedBase)
                if (seen.containsKey(id)) continue

                val relativeDir = resRoot.toPath().relativize(dir.toPath()).toString()
                    .replace(File.separatorChar, '/')
                seen[id] = DrawableResource(
                    id = id,
                    fileNameWithoutExtension = baseName,
                    extension = extension,
                    relativeDir = relativeDir,
                )
            }
        }
        return seen.values.toList().sortedBy { it.id }
    }

    private fun sanitizeIdentifier(raw: String): String {
        val cleaned = raw.lowercase().map { ch ->
            if (ch == '_' || ch.isLetterOrDigit()) ch else '_'
        }.joinToString("")
        if (cleaned.isEmpty()) return "res_unnamed"
        val first = cleaned.first()
        return if (first == '_' || first.isLetter()) cleaned else "res_$cleaned"
    }

    private fun writeCommonRes(
        outputDir: File,
        packageName: String,
        strings: List<StringResource>,
        drawables: List<DrawableResource>,
    ) {
        val file = outputDir.resolve("Res.kt")
        file.writeText(buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.compose.runtime.Composable")
            appendLine("import androidx.compose.ui.graphics.painter.Painter")
            appendLine()
            appendLine("expect object Res {")
            appendLine("    expect object strings {")
            if (strings.isEmpty()) {
                appendLine("    }")
            } else {
                for (string in strings) {
                    appendLine("        @Composable")
                    appendLine("        fun ${string.name}(): String")
                }
                appendLine("    }")
            }
            appendLine()
            appendLine("    expect object drawable {")
            if (drawables.isEmpty()) {
                appendLine("    }")
            } else {
                for (drawable in drawables) {
                    appendLine("        @Composable")
                    appendLine("        fun ${drawable.id}(): Painter")
                }
                appendLine("    }")
            }
            appendLine("}")
        })
    }

    private fun writeAndroidRes(
        outputDir: File,
        packageName: String,
        androidNamespace: String,
        strings: List<StringResource>,
        drawables: List<DrawableResource>,
    ) {
        val file = outputDir.resolve("Res.android.kt")
        file.writeText(buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.compose.runtime.Composable")
            appendLine("import androidx.compose.ui.graphics.painter.Painter")
            appendLine("import androidx.compose.ui.res.painterResource")
            appendLine("import androidx.compose.ui.res.stringResource")
            appendLine("import $androidNamespace.R")
            appendLine()
            appendLine("actual object Res {")
            appendLine("    actual object strings {")
            if (strings.isEmpty()) {
                appendLine("    }")
            } else {
                for (string in strings) {
                    appendLine("        @Composable")
                    appendLine("        actual fun ${string.name}(): String = stringResource(R.string.${string.name})")
                }
                appendLine("    }")
            }
            appendLine()
            appendLine("    actual object drawable {")
            if (drawables.isEmpty()) {
                appendLine("    }")
            } else {
                for (drawable in drawables) {
                    appendLine("        @Composable")
                    appendLine("        actual fun ${drawable.id}(): Painter = painterResource(R.drawable.${drawable.id})")
                }
                appendLine("    }")
            }
            appendLine("}")
        })
    }

    private fun writeIosRes(
        outputDir: File,
        packageName: String,
        iosPrefix: String,
        strings: List<StringResource>,
        drawables: List<DrawableResource>,
    ) {
        val file = outputDir.resolve("Res.ios.kt")
        file.writeText(buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.compose.runtime.Composable")
            appendLine("import androidx.compose.runtime.remember")
            appendLine("import androidx.compose.ui.geometry.Rect")
            appendLine("import androidx.compose.ui.graphics.Canvas")
            appendLine("import androidx.compose.ui.graphics.Color")
            appendLine("import androidx.compose.ui.graphics.ImageBitmap")
            appendLine("import androidx.compose.ui.graphics.Paint")
            appendLine("import androidx.compose.ui.graphics.asImageBitmap")
            appendLine("import androidx.compose.ui.graphics.painter.BitmapPainter")
            appendLine("import androidx.compose.ui.graphics.painter.ColorPainter")
            appendLine("import androidx.compose.ui.graphics.painter.Painter")
            appendLine("import cn.szkug.akit.compose.image.ImageBitmapNinePatchImage")
            appendLine("import cn.szkug.akit.compose.image.NinePatchPainter")
            appendLine("import cn.szkug.graphics.ninepatch.NinePatchChunk")
            appendLine("import kotlinx.cinterop.addressOf")
            appendLine("import kotlinx.cinterop.usePinned")
            appendLine("import org.jetbrains.skia.Image")
            appendLine("import platform.Foundation.NSBundle")
            appendLine("import platform.Foundation.NSData")
            appendLine("import platform.Foundation.dataWithContentsOfFile")
            appendLine("import platform.posix.memcpy")
            appendLine()
            appendLine("private val stringTable = mapOf(")
            if (strings.isEmpty()) {
                appendLine(")")
            } else {
                for ((index, string) in strings.withIndex()) {
                    val escaped = escapeKotlinString(string.value)
                    val suffix = if (index == strings.lastIndex) "" else ","
                    appendLine("    \"${string.name}\" to \"$escaped\"$suffix")
                }
                appendLine(")")
            }
            appendLine()
            appendLine("actual object Res {")
            appendLine("    actual object strings {")
            if (strings.isEmpty()) {
                appendLine("    }")
            } else {
                for (string in strings) {
                    appendLine("        @Composable")
                    appendLine("        actual fun ${string.name}(): String = stringTable[\"${string.name}\"] ?: \"\"")
                }
                appendLine("    }")
            }
            appendLine()
            appendLine("    actual object drawable {")
            if (drawables.isEmpty()) {
                appendLine("    }")
            } else {
                for (drawable in drawables) {
                    val dir = if (iosPrefix.isBlank()) drawable.relativeDir else "$iosPrefix/${drawable.relativeDir}"
                    appendLine("        @Composable")
                    appendLine(
                        "        actual fun ${drawable.id}(): Painter = remember { " +
                            "loadPainter(\"${drawable.fileNameWithoutExtension}\", \"${drawable.extension}\", \"$dir\") }"
                    )
                }
                appendLine("    }")
            }
            appendLine("}")
            appendLine()
            appendLine("private fun loadPainter(name: String, extension: String, directory: String): Painter {")
            appendLine("    val bitmap = loadImageBitmap(name, extension, directory) ?: return ColorPainter(Color.Transparent)")
            appendLine("    val ninePatchImage = ImageBitmapNinePatchImage(bitmap)")
            appendLine("    return if (NinePatchChunk.isRawNinePatchImage(ninePatchImage)) {")
            appendLine("        val chunk = NinePatchChunk.createChunkFromRawImage(ninePatchImage)")
            appendLine("        val cropped = cropNinePatch(bitmap)")
            appendLine("        NinePatchPainter(cropped, chunk)")
            appendLine("    } else {")
            appendLine("        BitmapPainter(bitmap)")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("private fun loadImageBitmap(name: String, extension: String, directory: String): ImageBitmap? {")
            appendLine("    val path = NSBundle.mainBundle.pathForResource(name, extension, directory) ?: return null")
            appendLine("    val data = NSData.dataWithContentsOfFile(path) ?: return null")
            appendLine("    val bytes = data.toByteArray()")
            appendLine("    if (bytes.isEmpty()) return null")
            appendLine("    val image = Image.makeFromEncoded(bytes)")
            appendLine("    return image.asImageBitmap()")
            appendLine("}")
            appendLine()
            appendLine("private fun NSData.toByteArray(): ByteArray {")
            appendLine("    val length = this.length.toInt()")
            appendLine("    if (length == 0) return ByteArray(0)")
            appendLine("    val source = this.bytes ?: return ByteArray(0)")
            appendLine("    val buffer = ByteArray(length)")
            appendLine("    buffer.usePinned { pinned ->")
            appendLine("        memcpy(pinned.addressOf(0), source, this.length)")
            appendLine("    }")
            appendLine("    return buffer")
            appendLine("}")
            appendLine()
            appendLine("private fun cropNinePatch(image: ImageBitmap): ImageBitmap {")
            appendLine("    val contentWidth = (image.width - 2).coerceAtLeast(1)")
            appendLine("    val contentHeight = (image.height - 2).coerceAtLeast(1)")
            appendLine("    val out = ImageBitmap(width = contentWidth, height = contentHeight)")
            appendLine("    val canvas = Canvas(out)")
            appendLine("    val paint = Paint()")
            appendLine("    val src = Rect(")
            appendLine("        left = 1f,")
            appendLine("        top = 1f,")
            appendLine("        right = (image.width - 1).toFloat(),")
            appendLine("        bottom = (image.height - 1).toFloat(),")
            appendLine("    )")
            appendLine("    val dst = Rect(")
            appendLine("        left = 0f,")
            appendLine("        top = 0f,")
            appendLine("        right = contentWidth.toFloat(),")
            appendLine("        bottom = contentHeight.toFloat(),")
            appendLine("    )")
            appendLine("    canvas.drawImageRect(image, src, dst, paint)")
            appendLine("    return out")
            appendLine("}")
        })
    }

    private fun escapeKotlinString(value: String): String {
        return buildString {
            for (ch in value) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }
}
