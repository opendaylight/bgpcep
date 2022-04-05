/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * BGP Message codec registry, provides services to encode/decode messages.
 */
@NonNullByDefault
public interface MessageRegistry {
    /**
     * Decode input buffer to BGP Message.
     * @param bytes Input buffer with encoded message.
     * @param constraint Peer specific constraint.
     * @return Parsed BGP message.
     */
    Notification<?> parseMessage(ByteBuf bytes, @Nullable PeerSpecificParserConstraint constraint)
            throws BGPDocumentedException, BGPParsingException;

    /**
     * Encode input BGP Message to output buffer.
     * @param message Input BGP Message to be serialized.
     * @param buffer Output buffer where message is to be written.
     */
    void serializeMessage(Notification<?> message, ByteBuf buffer);
}
