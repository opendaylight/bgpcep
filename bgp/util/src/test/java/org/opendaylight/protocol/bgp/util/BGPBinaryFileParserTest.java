/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class BGPBinaryFileParserTest {

    private static final byte FF = (byte) 255;

    private static List<byte[]> extractFromFile(final String fileName) throws IOException {
        final InputStream is = BGPBinaryFileParserTest.class.getResourceAsStream(fileName);
        if (is == null) {
            throw new IOException("Failed to get resource " + fileName);
        }

        final ByteArrayOutputStream bis = new ByteArrayOutputStream();
        final byte[] data = new byte[1000];
        int nread;
        while ((nread = is.read(data, 0, data.length)) != -1) {
            bis.write(data, 0, nread);
        }
        bis.flush();
        return BinaryBGPDumpFileParser.parseMessages(bis.toByteArray());
    }

    @Test
    void testCorrectExtraction() throws IOException {
        final List<byte[]> parsedMessages = extractFromFile("/BgpMessages.bin");

        assertEquals(43, parsedMessages.size());

        // 1st message
        assertEquals(19, parsedMessages.get(0).length);
        checkMarker(parsedMessages);
        assertEquals((byte) 0, parsedMessages.get(0)[16]);
        assertEquals((byte) 19, parsedMessages.get(0)[17]);
        assertEquals((byte) 4, parsedMessages.get(0)[18]);

        // 39th message
        assertEquals(91, parsedMessages.get(38).length);
        checkMarker(parsedMessages);
        assertEquals((byte) 0, parsedMessages.get(38)[16]);
        assertEquals((byte) 91, parsedMessages.get(38)[17]);
        assertEquals((byte) 2, parsedMessages.get(38)[18]);
        assertEquals((byte) 236, parsedMessages.get(38)[90]);

    }

    private void checkMarker(final List<byte[]> parsedMessages) {
        for (int i = 0; i < 16; i++) {
            assertEquals(FF, parsedMessages.get(0)[i]);
        }
    }

    /**
     * In BgpMessages_wrong_header file, first FF sequence is corrupted.
     */
    @Test
    void testCorruptedHeader() throws IOException {
        final List<byte[]> parsedMessages = extractFromFile("/BgpMessages_wrong_header.bin");
        assertEquals(42, parsedMessages.size());
    }
}
