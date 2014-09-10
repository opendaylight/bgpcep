/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.junit.Test;

public class PCEPHexDumpParserTest {

    public static final String hexDumpFileName = "pcep-hex.txt";
    private static final int expectedSize = 6;

    @Test
    public void testParsing() throws Exception {
        final List<byte[]> result = PCEPHexDumpParser.parseMessages(getClass().getClassLoader().getResourceAsStream(
            PCEPHexDumpParserTest.hexDumpFileName));
        assertEquals(expectedSize, result.size());
        final List<byte[]> result1 = PCEPHexDumpParser.parseMessages(new File(getClass().getClassLoader().getResource(
            PCEPHexDumpParserTest.hexDumpFileName).toURI()));
        assertEquals(expectedSize, result1.size());
    }

    @Test
    public void testParsingInvalidFile() throws Exception {
        try {
            PCEPHexDumpParser.parseMessages(new File("bad file name"));
            fail("Exception should have occured.");
        } catch (final FileNotFoundException e) {
            assertThat(e.getMessage(), containsString("bad file name"));
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<PCEPHexDumpParser> c = PCEPHexDumpParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
