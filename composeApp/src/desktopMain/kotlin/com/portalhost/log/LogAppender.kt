package com.portalhost.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.FileAppender
import org.slf4j.LoggerFactory
import java.io.File

class DesktopLogAppender : AppenderBase<ILoggingEvent>() {
    override fun append(event: ILoggingEvent) {
        val level = event.level.levelStr
        val loggerName = event.loggerName.substringAfterLast('.')
        val message = event.formattedMessage
        val throwable = event.throwableProxy?.message
        logRepository?.publish(level, loggerName, message, throwable)
    }

    companion object {
        var logRepository: LogRepository? = null
    }
}

fun setupLogging(logRepository: LogRepository) {
    DesktopLogAppender.logRepository = logRepository
    val context = LoggerFactory.getILoggerFactory() as LoggerContext
    val root = context.getLogger(Logger.ROOT_LOGGER_NAME)

    val appender = DesktopLogAppender()
    appender.context = context
    appender.start()
    root.addAppender(appender)

    // Also write an on-disk log (data dir first, temp as fallback) so future
    // "won't open" reports can be diagnosed even when the UI never appears.
    try {
        val dataDir = System.getProperty("portalhost.data.dir")?.takeIf { it.isNotBlank() }
            ?: com.portalhost.filesystem.defaultDataDir().absolutePath
        File(dataDir).mkdirs()
        val fileAppender = FileAppender<ILoggingEvent>()
        fileAppender.context = context
        fileAppender.name = "FILE"
        fileAppender.file = File(dataDir, "portalhost.log").absolutePath
        fileAppender.isAppend = true
        val encoder = PatternLayoutEncoder()
        encoder.context = context
        encoder.pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"
        encoder.start()
        fileAppender.encoder = encoder
        fileAppender.start()
        root.addAppender(fileAppender)
    } catch (_: Exception) { }
}
