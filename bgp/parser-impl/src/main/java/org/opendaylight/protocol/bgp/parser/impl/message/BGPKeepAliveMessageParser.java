/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.KeepaliveBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class BGPKeepAliveMessageParser implements MessageParser, MessageSerializer {
    public static final int TYPE = 4;
    private static final Keepalive KEEPALIVE_MSG = new KeepaliveBuilder().build();
    private static final ByteBuf KEEPALIVE_BYTES = Unpooled.buffer();

    static {
        MessageUtil.formatMessage(TYPE, Unpooled.EMPTY_BUFFER,KEEPALIVE_BYTES);
    }

    @Override
    public Keepalive parseMessageBody(final ByteBuf body, final int messageLength) throws BGPDocumentedException {
        if (body.isReadable()) {
            throw BGPDocumentedException.badMessageLength("Message length field not within valid range.", messageLength);
        }
        return KEEPALIVE_MSG;
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf bytes) {
        Preconditions.checkArgument(message instanceof Keepalive);
        bytes.writeBytes(KEEPALIVE_BYTES.slice());
    }
}
