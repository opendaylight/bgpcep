/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertEquals;

import ch.qos.logback.classic.Level;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtilTest {
    @Test
    public void initiateLogger() {
        LoggerUtil.initiateLogger(() -> Level.DEBUG);
        final ch.qos.logback.classic.Logger protocolLogger
                = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.opendaylight.protocol");
        assertEquals(protocolLogger.getLevel(), Level.DEBUG);

        final ch.qos.logback.classic.Logger logger
                = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        assertEquals(logger.getLevel(), Level.OFF);
    }
}