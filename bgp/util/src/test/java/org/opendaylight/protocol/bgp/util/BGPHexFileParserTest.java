/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class BGPHexFileParserTest {

    private static final String HEX_DUMP_FILE_NAME = "bgp_hex.txt";
    private static final String BGP_MESSAGE_HEX_INVALID_LENGTH_BIN = "BgpMessage_Hex_InvalidLength.bin";
    private static final int EXPECTED_SIZE = 25;

    @Test
    void testCleanWhiteSpace() {
        final String input = "abc def\r\nghi\nj";
        assertEquals("ABCDEFGHIJ", HexDumpBGPFileParser.clearWhiteSpaceToUpper(input));
    }

    @Test
    void testParsing() throws Exception {
        final List<byte[]> result = HexDumpBGPFileParser.parseMessages(getClass().getClassLoader()
                .getResourceAsStream(BGPHexFileParserTest.HEX_DUMP_FILE_NAME));
        assertEquals(EXPECTED_SIZE, result.size());
    }

    @Test
    void testParsingInvalidMessage() {
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> HexDumpBGPFileParser.parseMessages(getClass().getClassLoader()
                    .getResourceAsStream(BGP_MESSAGE_HEX_INVALID_LENGTH_BIN)));
        assertEquals("Invalid message at index 0, length atribute is lower than 19", ex.getMessage());
    }

    @Test
    void testParsingInvalidFile() {
        final var ex = assertThrows(NoSuchFileException.class,
            () -> HexDumpBGPFileParser.parseMessages(Path.of("bad file name")));
        assertEquals("bad file name", ex.getMessage());
    }
}
