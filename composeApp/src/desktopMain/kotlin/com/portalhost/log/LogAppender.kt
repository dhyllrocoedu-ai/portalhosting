package com.portalhost.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.slf4j.LoggerFactory

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
}
