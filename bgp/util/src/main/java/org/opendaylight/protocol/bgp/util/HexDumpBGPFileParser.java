/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read text file, parse BGP messages. File can contain comments or other data. BGP messages are detected using 16 ff
 * marker. New lines and spaces are ignored. Use {@link ByteArray#bytesToHexString(byte[])} for serializing bytes to
 * this format.
 */
@Immutable
public final class HexDumpBGPFileParser {
    private static final int MINIMAL_LENGTH = 19;
    private static final Logger LOG = LoggerFactory.getLogger(HexDumpBGPFileParser.class);
    private static final String FF_16 = Strings.repeat("FF", 16);

    private HexDumpBGPFileParser() {

    }

    public static List<byte[]> parseMessages(final File file) throws IOException {
        Preconditions.checkArgument(file != null, "Filename cannot be null");
        return parseMessages(new FileInputStream(file));
    }

    public static List<byte[]> parseMessages(final InputStream is) throws IOException {
        Preconditions.checkNotNull(is);
        try (InputStreamReader isr = new InputStreamReader(is)) {
            return parseMessages(CharStreams.toString(isr));
        } finally {
            is.close();
        }
    }

    public static List<byte[]> parseMessages(final String c) {
        final String content = clearWhiteSpaceToUpper(c);
        // search for 16 FFs

        final List<byte[]> messages = Lists.newLinkedList();
        int idx = content.indexOf(FF_16, 0);
        while (idx > -1) {
            // next 2 bytes are length
            final int lengthIdx = idx + 16 * 2;
            final int messageIdx = lengthIdx + 4;
            final String hexLength = content.substring(lengthIdx, messageIdx);
            byte[] byteLength = null;
            try {
                byteLength = Hex.decodeHex(hexLength.toCharArray());
            } catch (final DecoderException e) {
                throw new IllegalArgumentException("Failed to decode message length", e);
            }
            final int length = ByteArray.bytesToInt(byteLength);
            final int messageEndIdx = idx + length * 2;

            // Assert that message is longer than minimum 19(header.length == 19)
            // If length in BGP message would be 0, loop would never end
            Preconditions.checkArgument(length >= MINIMAL_LENGTH, "Invalid message at index " + idx + ", length atribute is lower than "
                    + MINIMAL_LENGTH);

            final String hexMessage = content.substring(idx, messageEndIdx);
            byte[] message = null;
            try {
                message = Hex.decodeHex(hexMessage.toCharArray());
            } catch (final DecoderException e) {
                throw new IllegalArgumentException("Failed to decode message body", e);
            }
            messages.add(message);
            idx = messageEndIdx;
            idx = content.indexOf(FF_16, idx);
        }
        LOG.info("Succesfully extracted {} messages", messages.size());
        return messages;
    }

    @VisibleForTesting
    static String clearWhiteSpaceToUpper(final String line) {
        return line.replaceAll("\\s", "").toUpperCase();
    }
}
