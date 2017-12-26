/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;
import java.util.Arrays;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This extracter extracts BGP messages in binary form from a file in MRT format.
 * (http://www.ripe.net/data-tools/stats/ris/ris-raw-data) The parser detects BGP messages by searching for 16 FF bytes,
 * everything else before or after is ignored.
 */
@Immutable
public final class BinaryBGPDumpFileParser {
    private static final Logger LOG = LoggerFactory.getLogger(BinaryBGPDumpFileParser.class);
    private static final int MINIMAL_LENGTH = 19;
    private static final int MARKER_LENGTH = 16;

    private BinaryBGPDumpFileParser() {
        throw new UnsupportedOperationException();
    }

    /**
     * Extract BGP messages from binary file in MRT format.
     *
     * @param byteArray array of bytes with BGP messages in binary form.
     * @return list with byte arrays representing extracted messages.
     */
    public static List<byte[]> parseMessages(final byte[] byteArray) {

        final List<byte[]> messages = Lists.newLinkedList();
        // search for 16 FFs
        for (int i = 0; i < byteArray.length; i++) {
            final byte b = byteArray[i];

            // Marker start
            if (b == UnsignedBytes.MAX_VALUE) {
                final int start = i;
                int ffCount = 0;
                for (int j = i; j <= i + MARKER_LENGTH; j++) {
                    // Check marker
                    if (byteArray[j] == UnsignedBytes.MAX_VALUE) {
                        ffCount++;
                    } else if (ffCount == MARKER_LENGTH) {
                        if (j == (i + MARKER_LENGTH)) {
                            // Parse length
                            final int length = ByteArray.bytesToInt(new byte[]{ byteArray[j], byteArray[j + 1] });

                            Preconditions.checkArgument(length >= MINIMAL_LENGTH,
                                    "Invalid message at index " + start
                                    + ", length atribute is lower than " + MINIMAL_LENGTH);

                            final byte[] message = Arrays.copyOfRange(byteArray, start, start + length);
                            messages.add(message);
                            j += length - MARKER_LENGTH;
                        }
                        i = j;
                        break;
                    } else {
                        break;
                    }
                }
            }

        }
        LOG.info("Succesfully extracted {} messages", messages.size());
        return messages;
    }
}
