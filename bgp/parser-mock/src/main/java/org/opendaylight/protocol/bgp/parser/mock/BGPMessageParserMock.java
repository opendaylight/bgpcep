/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.mock;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Mock implementation of {@link MessageRegistry}. It implements the required interface by having two internal maps,
 * each used in one of the methods. It looks up the key provided to the method and returns whatever value is stored in
 * the map.
 */
public class BGPMessageParserMock implements MessageRegistry {
    private final Map<ByteBuf, Notification<?>> messages;

    /**
     * Creates a new BGPMessageParserMock with given messages.
     *
     * @param messages represents a new map of ByteBuf and Notification
     */
    public BGPMessageParserMock(final Map<ByteBuf, Notification<?>> messages) {
        this.messages = messages;
    }

    @Override
    public void serializeMessage(final Notification<?> msg, final ByteBuf buffer) {
        // no action needed, it's a mock for parsing, not serializing
    }

    @Override
    public Notification<?> parseMessage(final ByteBuf buffer, final PeerSpecificParserConstraint constraint)
            throws BGPDocumentedException, BGPParsingException {
        final Notification<?> ret = messages.get(buffer);
        Preconditions.checkArgument(ret != null, "Undefined message encountered");
        return ret;
    }
}
