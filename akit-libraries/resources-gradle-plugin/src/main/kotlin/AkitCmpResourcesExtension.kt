import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class AkitCmpResourcesExtension {
    abstract val packageName: Property<String>
    abstract val androidNamespace: Property<String>
    abstract val resDir: DirectoryProperty
    abstract val androidExtraResDir: DirectoryProperty
    abstract val iosExtraResDir: DirectoryProperty
    abstract val iosResourcesPrefix: Property<String>
    abstract val whitelistEnabled: Property<Boolean>
    abstract val stringsWhitelistFile: RegularFileProperty
    abstract val drawablesWhitelistFile: RegularFileProperty
}
