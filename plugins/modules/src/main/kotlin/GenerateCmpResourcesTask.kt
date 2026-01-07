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

    @get:Input
    abstract val iosFrameworkName: Property<String>

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
        val iosFrameworkName = iosFrameworkName.get()

        writeCommonRes(commonDir, pkg, strings, drawables)
        writeAndroidRes(androidDir, pkg, androidPkg, strings, drawables)
        writeIosRes(iosDir, pkg, iosPrefix, iosFrameworkName, strings, drawables)
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
            appendLine("    object strings {")
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
            appendLine("    object drawable {")
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
            appendLine("import androidx.appcompat.content.res.AppCompatResources")
            appendLine("import androidx.compose.runtime.Composable")
            appendLine("import androidx.compose.runtime.remember")
            appendLine("import androidx.compose.ui.graphics.Color")
            appendLine("import androidx.compose.ui.graphics.painter.ColorPainter")
            appendLine("import androidx.compose.ui.graphics.painter.Painter")
            appendLine("import androidx.compose.ui.platform.LocalContext")
            appendLine("import androidx.compose.ui.res.stringResource")
            appendLine("import cn.szkug.akit.publics.toPainter")
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
                    appendLine("        actual fun ${drawable.id}(): Painter {")
                    appendLine("            val context = LocalContext.current")
                    appendLine("            val drawable = remember(context) {")
                    appendLine("                AppCompatResources.getDrawable(context, R.drawable.${drawable.id})")
                    appendLine("            }")
                    appendLine("            return drawable?.toPainter() ?: ColorPainter(Color.Transparent)")
                    appendLine("        }")
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
        iosFrameworkName: String,
        strings: List<StringResource>,
        drawables: List<DrawableResource>,
    ) {
        val file = outputDir.resolve("Res.ios.kt")
        file.writeText(buildString {
            appendLine("@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)")
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.compose.runtime.Composable")
            appendLine("import androidx.compose.runtime.remember")
            appendLine("import androidx.compose.ui.graphics.Canvas")
            appendLine("import androidx.compose.ui.graphics.ImageBitmap")
            appendLine("import androidx.compose.ui.graphics.Paint")
            appendLine("import androidx.compose.ui.graphics.toComposeImageBitmap")
            appendLine("import androidx.compose.ui.graphics.painter.BitmapPainter")
            appendLine("import androidx.compose.ui.graphics.painter.Painter")
            appendLine("import androidx.compose.ui.unit.IntOffset")
            appendLine("import androidx.compose.ui.unit.IntSize")
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
            appendLine("private const val frameworkName = \"${iosFrameworkName}\"")
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
            appendLine("private fun resourceBundle(): NSBundle {")
            appendLine("    if (frameworkName.isBlank()) return NSBundle.mainBundle")
            appendLine("    val frameworksPath = NSBundle.mainBundle.privateFrameworksPath ?: return NSBundle.mainBundle")
            appendLine("    val frameworkPath = \"\$frameworksPath/\$frameworkName.framework\"")
            appendLine("    return NSBundle.bundleWithPath(frameworkPath) ?: NSBundle.mainBundle")
            appendLine("}")
            appendLine()
            appendLine("private fun loadPainter(name: String, extension: String, directory: String): Painter {")
            appendLine("    val bitmap = loadImageBitmap(name, extension, directory)")
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
            appendLine("private fun loadImageBitmap(name: String, extension: String, directory: String): ImageBitmap {")
            appendLine("    val bundle = resourceBundle()")
            appendLine("    val path = if (directory.isNotBlank()) {")
            appendLine("        bundle.pathForResource(name, extension, directory)")
            appendLine("            ?: bundle.pathForResource(name, extension)")
            appendLine("    } else {")
            appendLine("        bundle.pathForResource(name, extension)")
            appendLine("    }")
            appendLine("    if (path == null) {")
            appendLine("        error(\"Failed to resolve image path: name=\$name extension=\$extension directory=\$directory\")")
            appendLine("    }")
            appendLine("    val data = NSData.dataWithContentsOfFile(path)")
            appendLine("        ?: error(\"Failed to load image data: path=\$path\")")
            appendLine("    val bytes = data.toByteArray()")
            appendLine("    if (bytes.isEmpty()) {")
            appendLine("        error(\"Image data is empty: path=\$path\")")
            appendLine("    }")
            appendLine("    val image = Image.makeFromEncoded(bytes)")
            appendLine("    return image.toComposeImageBitmap()")
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
            appendLine("    val srcOffset = IntOffset(1, 1)")
            appendLine("    val srcSize = IntSize(contentWidth, contentHeight)")
            appendLine("    val dstOffset = IntOffset(0, 0)")
            appendLine("    val dstSize = IntSize(contentWidth, contentHeight)")
            appendLine("    canvas.drawImageRect(image, srcOffset, srcSize, dstOffset, dstSize, paint)")
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
