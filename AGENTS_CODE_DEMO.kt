// 这是一个代码设计、注释风格的参考文件

import android.net.Uri
import androidx.core.net.toUri

/**
 * OSS 资源 Url 参数构建器
 *
 * @params uri 原本的资源 Uri
 * @param type OSS 的资源处理类型，例如 image、video 等
 */
class OSSUrlBuilder(uri: Uri, var type: String) {

    companion object {
        private const val OSS_PROCESS_KEY = "x-oss-process"

        /**
         * 从 Url 字符串解析为 OSSUrlBuilder，如果 url 参数不合法则返回 null
         */
        fun parse(url: String, type: String): OSSUrlBuilder? {
            // 可能外部传入的不是一个合法的 url，这时候转化成 Uri 会崩溃。
            // 这种情况下没什么特别好的兼容方案，因此直接返回 null
            val uri = runCatching {
                url.toUri()
            }.getOrNull() ?: return null
            return OSSUrlBuilder(uri, type)
        }
    }

    /**
     * 可能存在一些参数清空场景，因此字段需要可修改用于覆盖原本的 builder
     * @see replaceQuery
     */
    private var builder = uri.buildUpon()

    /**
     * OSS 的参数格式为 type/action,value/action,value,value...
     *
     * 因此采用一个 Action Map + Value List 的结构来保存临时参数
     */
    private val actions = mutableMapOf<String, Array<out String>>()

    /**
     * 替换某个 query 参数，如果已经有相同 key 的参数，会覆盖原本的参数。
     *
     * WARN：外部调用应该禁止传入 key 为 [OSS_PROCESS_KEY] 的参数修改，
     * 因为 [OSS_PROCESS_KEY] 的 Query 最终都会在 build 的时候被替换掉
     */
    fun replaceQuery(key: String, value: String) = apply {
        // 由于 Uri 的 Builder 不存在查询方法，
        // 因此需要构建出当前的 Uri 用于查询已有参数
        val currentUri = builder.build()
        // 并重新创建一个清空了 query 的 Builder 来用于后续同参数的覆盖
        val newBuilder = currentUri.buildUpon().clearQuery()

        for (oriKey in currentUri.queryParameterNames) {
            // 优先添加其它参数，新参数留到最后添加
            if (oriKey == key) continue

            // Uri 中，一个参数的 key 可能存在多个 value
            for (value in currentUri.getQueryParameters(oriKey)) {
                newBuilder.appendQueryParameter(oriKey, value)
            }
        }

        newBuilder.appendQueryParameter(key, value)
        builder = newBuilder
    }

    /**
     * 替换 OSS 的 x-oss-process 参数。
     * 如果已经有 x-oss-process 参数，会覆盖原本的参数。
     */
    private fun appendOSSProcessQuery(process: String) = replaceQuery(OSS_PROCESS_KEY, process)

    private fun appendOSSActions(action: String, vararg values: String) = apply {
        actions[action] = values
    }

    /**
     * 添加 OSS 图片 resize 缩放参数
     */
    fun resize(resize: String): OSSUrlBuilder = appendOSSActions("resize", resize)

    /**
     * 添加 OSS 快照 snapshot 参数
     */
    fun snapshot(vararg values: String): OSSUrlBuilder = appendOSSActions("snapshot", *values)

    /**
     * 构建 OSS 资源的 Uri
     */
    fun build(): Uri {
        val process = StringBuilder(type)
        actions.forEach { (key, values) ->
            val action = key + "," + values.joinToString(",")
            process.append("/$action")
        }
        appendOSSProcessQuery(process.toString())
        return builder.build()
    }
}