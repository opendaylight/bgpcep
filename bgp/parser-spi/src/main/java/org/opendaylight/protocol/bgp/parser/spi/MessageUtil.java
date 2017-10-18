/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;

public final class MessageUtil {

    @VisibleForTesting
    public static final int MARKER_LENGTH = 16;
    @VisibleForTesting
    public static final int COMMON_HEADER_LENGTH = 19;
    private static final byte[] MARKER = new byte[MARKER_LENGTH];

    static {
        Arrays.fill(MARKER, 0, MARKER_LENGTH, UnsignedBytes.MAX_VALUE);
    }

    private MessageUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds header to message value.
     *
     * @param type of the message
     * @param body message body
     * @param buffer ByteBuf where the message will be copied with its header
     */
    public static void formatMessage(final int type, final ByteBuf body, final ByteBuf buffer) {
        buffer.writeBytes(MARKER);
        buffer.writeShort(body.writerIndex() + COMMON_HEADER_LENGTH);
        buffer.writeByte(type);
        buffer.writeBytes(body);
    }

    /**
     * Check for NLRI attribute in Update message
     *
     * @param message Update message
     * @return true if any prefix or MP-REACH-NLRI attribute is present, false otherwise
     */
    public static boolean isAnyNlriPresent(final Update message) {
        if (message == null || message.getAttributes() == null) {
            return false;
        }
        final List<Nlri> nlri = message.getNlri();
        return nlri != null && !nlri.isEmpty()
            || getMpReachNlri(message.getAttributes()) != null;
    }

    /**
     * Finds MP-REACH-NLRI in Update message attributes
     *
     * @param attrs Update message attributes
     * @return MP-REACH-NLRI if present in the attributes, null otherwise
     */
    public static MpReachNlri getMpReachNlri(final Attributes attrs) {
        if (attrs != null && attrs.getAugmentation(Attributes1.class) != null) {
            return attrs.getAugmentation(Attributes1.class).getMpReachNlri();
        }

        return null;
    }

    /**
     * Finds MP-UNREACH-NLRI in Update message attributes
     *
     * @param attrs Update message attributes
     * @return MP-UNREACH-NLRI if present in the attributes, null otherwise
     */
    public static MpUnreachNlri getMpUnreachNlri(final Attributes attrs) {
        if (attrs != null && attrs.getAugmentation(Attributes2.class) != null) {
            return attrs.getAugmentation(Attributes2.class).getMpUnreachNlri();
        }

        return null;
    }
}
