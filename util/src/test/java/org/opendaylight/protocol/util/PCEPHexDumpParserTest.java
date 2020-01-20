/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import org.junit.Test;

public class PCEPHexDumpParserTest {

    private static final String HEX_DUMP_FILE_NAME = "pcep-hex.txt";
    private static final int EXPECTED_SIZE = 6;

    @Test
    public void testParsing() throws Exception {
        final List<byte[]> result = PCEPHexDumpParser.parseMessages(getClass().getClassLoader().getResourceAsStream(
                PCEPHexDumpParserTest.HEX_DUMP_FILE_NAME));
        assertEquals(EXPECTED_SIZE, result.size());
        final List<byte[]> result1 = PCEPHexDumpParser.parseMessages(new File(getClass().getClassLoader().getResource(
                PCEPHexDumpParserTest.HEX_DUMP_FILE_NAME).toURI()));
        assertEquals(EXPECTED_SIZE, result1.size());
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
}
