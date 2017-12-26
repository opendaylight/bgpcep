/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import org.junit.Test;

public class BGPHexFileParserTest {

    private static final String HEX_DUMP_FILE_NAME = "bgp_hex.txt";
    private static final String BGP_MESSAGE_HEX_INVALID_LENGTH_BIN = "BgpMessage_Hex_InvalidLength.bin";
    private static final int EXPECTED_SIZE = 25;

    @Test
    public void testCleanWhiteSpace() {
        final String input = "abc def\r\nghi\nj";
        assertEquals("ABCDEFGHIJ", HexDumpBGPFileParser.clearWhiteSpaceToUpper(input));
    }

    @Test
    public void testParsing() throws Exception {
        final List<byte[]> result = HexDumpBGPFileParser.parseMessages(getClass().getClassLoader()
                .getResourceAsStream(BGPHexFileParserTest.HEX_DUMP_FILE_NAME));
        assertEquals(EXPECTED_SIZE, result.size());
    }

    @Test
    public void testParsingInvalidMessage() throws Exception {
        try {
            HexDumpBGPFileParser.parseMessages(getClass().getClassLoader()
                    .getResourceAsStream(BGP_MESSAGE_HEX_INVALID_LENGTH_BIN));
            fail("Exception should have occured.");
        } catch (final IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Invalid message at index 0, "
                    + "length atribute is lower than 19"));
        }
    }

    @Test
    public void testParsingInvalidFile() throws Exception {
        try {
            HexDumpBGPFileParser.parseMessages(new File("bad file name"));
            fail("Exception should have occured.");
        } catch (final FileNotFoundException e) {
            assertThat(e.getMessage(), containsString("bad file name"));
        }
    }
}
