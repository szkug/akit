import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class CmpResourcesExtension {
    abstract val packageName: Property<String>
    abstract val androidNamespace: Property<String>
    abstract val resDir: DirectoryProperty
    abstract val iosResourcesPrefix: Property<String>
    abstract val iosFrameworkName: Property<String>
    abstract val iosFrameworkBundleId: Property<String>
}
