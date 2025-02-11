/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read text file, parse BGP messages. File can contain comments or other data. BGP messages are detected using 16 ff
 * marker. New lines and spaces are ignored.
 */
public final class HexDumpBGPFileParser {
    private static final int MINIMAL_LENGTH = 19;
    private static final Logger LOG = LoggerFactory.getLogger(HexDumpBGPFileParser.class);
    private static final String FF_16 = "FF".repeat(16);

    private HexDumpBGPFileParser() {
        // Hidden on purpose
    }

    @Deprecated
    public static List<byte[]> parseMessages(final File file) throws IOException {
        return parseMessages(file.toPath());
    }

    public static List<byte[]> parseMessages(final Path file) throws IOException {
        return parseMessages(Files.readString(file));
    }

    public static List<byte[]> parseMessages(final InputStream is) throws IOException {
        try {
            return parseMessages(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            is.close();
        }
    }

    public static List<byte[]> parseMessages(final String stringMessage) {
        final String content = clearWhiteSpaceToUpper(stringMessage);
        final int sixteen = 16;
        final int four = 4;
        // search for 16 FFs

        final var messages = new ArrayList<byte[]>();
        int idx = content.indexOf(FF_16, 0);
        while (idx > -1) {
            // next 2 bytes are length
            final int lengthIdx = idx + sixteen * 2;
            final int messageIdx = lengthIdx + four;
            final int length = ByteArray.bytesToInt(HexFormat.of().parseHex(content, lengthIdx, messageIdx));
            final int messageEndIdx = idx + length * 2;

            // Assert that message is longer than minimum 19(header.length == 19)
            // If length in BGP message would be 0, loop would never end
            if (length < MINIMAL_LENGTH) {
                throw new IllegalArgumentException("Invalid message at index " + idx
                    + ", length atribute is lower than " + MINIMAL_LENGTH);
            }

            final byte[] message = HexFormat.of().parseHex(content, idx, messageEndIdx);
            messages.add(message);
            idx = messageEndIdx;
            idx = content.indexOf(FF_16, idx);
        }
        LOG.info("Succesfully extracted {} messages", messages.size());
        return messages;
    }

    @VisibleForTesting
    static String clearWhiteSpaceToUpper(final String line) {
        return line.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }
}
