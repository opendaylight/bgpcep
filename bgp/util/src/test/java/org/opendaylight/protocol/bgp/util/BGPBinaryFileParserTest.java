/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;

public class BGPBinaryFileParserTest {

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
    public void testCorrectExtraction() throws IOException {
        final List<byte[]> parsedMessages = extractFromFile("/BgpMessages.bin");

        assertThat(parsedMessages.size(), is(43));

        // 1st message
        assertThat(parsedMessages.get(0).length, is(19));
        checkMarker(parsedMessages);
        assertThat(parsedMessages.get(0)[16], is((byte) 0));
        assertThat(parsedMessages.get(0)[17], is((byte) 19));
        assertThat(parsedMessages.get(0)[18], is((byte) 4));

        // 39th message
        assertThat(parsedMessages.get(38).length, is(91));
        checkMarker(parsedMessages);
        assertThat(parsedMessages.get(38)[16], is((byte) 0));
        assertThat(parsedMessages.get(38)[17], is((byte) 91));
        assertThat(parsedMessages.get(38)[18], is((byte) 2));
        assertThat(parsedMessages.get(38)[90], is((byte) 236));

    }

    private void checkMarker(final List<byte[]> parsedMessages) {
        for (int i = 0; i < 16; i++) {
            assertThat(parsedMessages.get(0)[i], is(FF));
        }
    }

    /**
     * In BgpMessages_wrong_header file, first FF sequence is corrupted.
     */
    @Test
    public void testCorruptedHeader() throws IOException {
        final List<byte[]> parsedMessages = extractFromFile("/BgpMessages_wrong_header.bin");
        assertEquals(42, parsedMessages.size());
    }
}
