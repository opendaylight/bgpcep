/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PCEPHexDumpParserTest {
    private static final String HEX_DUMP_FILE_NAME = "pcep-hex.txt";
    private static final int EXPECTED_SIZE = 6;

    @Test
    void testParsing() throws Exception {
        final var result = PCEPHexDumpParser.parseMessages(getClass().getClassLoader().getResourceAsStream(
                HEX_DUMP_FILE_NAME));
        assertEquals(EXPECTED_SIZE, result.size());
        final var result1 = PCEPHexDumpParser.parseMessages(Path.of(getClass().getClassLoader().getResource(
                HEX_DUMP_FILE_NAME).toURI()));
        assertEquals(EXPECTED_SIZE, result1.size());
    }

    @Test
    void testParsingInvalidFile() {
        final var ex = assertThrows(NoSuchFileException.class,
            () -> PCEPHexDumpParser.parseMessages(Path.of("bad file name")));
        assertEquals("bad file name", ex.getMessage());
    }
}
