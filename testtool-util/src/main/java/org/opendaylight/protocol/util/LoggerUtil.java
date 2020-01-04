/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggerUtil {
    private LoggerUtil() {
        // Hidden on purpose
    }

    public static void initiateLogger(final ArgumentsInput arguments) {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final ConsoleAppender<ILoggingEvent> consoleAppender = createConsoleAppender(loggerContext);
        setLogLevel(consoleAppender, Level.OFF, Logger.ROOT_LOGGER_NAME);
        setLogLevel(consoleAppender, arguments.getLogLevel(), "org.opendaylight.protocol");
    }

    private static ConsoleAppender<ILoggingEvent> createConsoleAppender(final LoggerContext loggerContext) {
        final PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{10} - %msg%n");
        ple.setContext(loggerContext);
        ple.start();

        final ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setEncoder(ple);
        consoleAppender.setName("STDOUT");
        consoleAppender.start();
        return consoleAppender;
    }

    private static void setLogLevel(final ConsoleAppender<ILoggingEvent> consoleAppender,
            final Level level, final String clazz) {
        final ch.qos.logback.classic.Logger protocolLogger
                = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(clazz);
        protocolLogger.addAppender(consoleAppender);
        protocolLogger.setLevel(level);
        protocolLogger.setAdditive(false);
    }
}
