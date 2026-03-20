package munchkin.resources.runtime

actual object DefaultPlatformMunchkinLogger : MunchkinLogger {

    private var level = MunchkinLogger.Level.ERROR

    actual override fun setLevel(level: MunchkinLogger.Level) {
        this.level = level
    }

    actual override fun debug(feature: String, message: () -> String) {
        if (MunchkinLogger.Level.DEBUG < level) return
        log(feature, message())
    }

    actual override fun info(feature: String, message: () -> String) {
        if (MunchkinLogger.Level.INFO < level) return
        log(feature, message())
    }

    actual override fun warn(feature: String, message: String) {
        if (MunchkinLogger.Level.WARN < level) return
        log(feature, message)
    }

    actual override fun error(feature: String, exception: Exception?) {
        if (MunchkinLogger.Level.ERROR < level || exception == null) return
        log(feature, "${exception::class.simpleName}: ${exception.message.orEmpty()}")
        log(feature, exception.stackTraceToString())
    }

    actual override fun error(feature: String, message: String) {
        if (MunchkinLogger.Level.ERROR < level) return
        log(feature, message)
    }

    private fun log(feature: String, message: String) {
        println("Munchkin [$feature] $message")
    }
}
