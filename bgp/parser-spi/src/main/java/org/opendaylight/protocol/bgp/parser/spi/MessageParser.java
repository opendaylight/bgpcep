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
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Common interface for message parser implementation.
 */
@NonNullByDefault
public interface MessageParser {
    /**
     * Parse BGP Message from buffer, potentially applying peer-specific constraints. Implementations are free
     *
     * @param body Encoded BGP message in ByteBuf.
     * @param messageLength Length of the BGP message.
     * @param constraint Peer specific constraints, implementations may ignore them.
     * @return Parsed BGP Message body.
     */
    Notification<?> parseMessageBody(ByteBuf body, int messageLength, @Nullable PeerSpecificParserConstraint constraint)
            throws BGPDocumentedException;
}
