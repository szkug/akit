package munchkin.resources.runtime

interface MunchkinLogger {

    enum class Level {
        DEBUG, INFO, WARN, ERROR,
    }

    fun setLevel(level: Level)
    fun debug(feature: String, message: () -> String)
    fun info(feature: String, message: () -> String)
    fun warn(feature: String, message: String)
    fun error(feature: String, exception: Exception?)
    fun error(feature: String, message: String)
}

expect object DefaultPlatformMunchkinLogger : MunchkinLogger {
    override fun setLevel(level: MunchkinLogger.Level)
    override fun debug(feature: String, message: () -> String)
    override fun info(feature: String, message: () -> String)
    override fun warn(feature: String, message: String)
    override fun error(feature: String, exception: Exception?)
    override fun error(feature: String, message: String)
}
