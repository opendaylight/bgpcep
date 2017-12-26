/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import javax.annotation.concurrent.Immutable;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read text file, parse BGP messages. File can contain comments or other data. BGP messages are detected using 16 ff
 * marker. New lines and spaces are ignored.
 */
@Immutable
public final class HexDumpBGPFileParser {
    private static final int MINIMAL_LENGTH = 19;
    private static final Logger LOG = LoggerFactory.getLogger(HexDumpBGPFileParser.class);
    private static final String FF_16 = Strings.repeat("FF", 16);

    private HexDumpBGPFileParser() {
        throw new UnsupportedOperationException();
    }

    public static List<byte[]> parseMessages(final File file) throws IOException {
        Preconditions.checkArgument(file != null, "Filename cannot be null");
        return parseMessages(new FileInputStream(file));
    }

    public static List<byte[]> parseMessages(final InputStream is) throws IOException {
        requireNonNull(is);
        try (InputStreamReader isr = new InputStreamReader(is, "UTF-8")) {
            return parseMessages(CharStreams.toString(isr));
        } finally {
            is.close();
        }
    }

    public static List<byte[]> parseMessages(final String stringMessage) {
        final String content = clearWhiteSpaceToUpper(stringMessage);
        final int sixteen = 16;
        final int four = 4;
        // search for 16 FFs

        final List<byte[]> messages = Lists.newLinkedList();
        int idx = content.indexOf(FF_16, 0);
        while (idx > -1) {
            // next 2 bytes are length
            final int lengthIdx = idx + sixteen * 2;
            final int messageIdx = lengthIdx + four;
            final String hexLength = content.substring(lengthIdx, messageIdx);
            final byte[] byteLength = BaseEncoding.base16().decode(hexLength);
            final int length = ByteArray.bytesToInt(byteLength);
            final int messageEndIdx = idx + length * 2;

            // Assert that message is longer than minimum 19(header.length == 19)
            // If length in BGP message would be 0, loop would never end
            Preconditions.checkArgument(length >= MINIMAL_LENGTH, "Invalid message at index "
                    + idx + ", length atribute is lower than " + MINIMAL_LENGTH);

            final String hexMessage = content.substring(idx, messageEndIdx);
            final byte[] message = BaseEncoding.base16().decode(hexMessage);
            messages.add(message);
            idx = messageEndIdx;
            idx = content.indexOf(FF_16, idx);
        }
        LOG.info("Succesfully extracted {} messages", messages.size());
        return messages;
    }

    @VisibleForTesting
    static String clearWhiteSpaceToUpper(final String line) {
        return line.replaceAll("\\s", "").toUpperCase(Locale.ENGLISH);
    }
}
