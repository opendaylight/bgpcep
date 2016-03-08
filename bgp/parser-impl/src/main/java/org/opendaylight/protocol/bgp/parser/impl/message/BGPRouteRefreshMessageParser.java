/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.RouteRefreshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPRouteRefreshMessageParser implements MessageParser, MessageSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(BGPRouteRefreshMessageParser.class);

    // https://tools.ietf.org/html/rfc2918#section-3
    public static final int TYPE = 5;
    private static final int TRIPLET_BYTE_SIZE = 4;
    private static final int RESERVED = 1;

    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public BGPRouteRefreshMessageParser(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = Preconditions.checkNotNull(afiReg);
        this.safiReg = Preconditions.checkNotNull(safiReg);
    }

    /**
     * Serializes BGP Route Refresh message.
     *
     * @param msg to be serialized
     * @param bytes ByteBuf where the message will be serialized
     */
    @Override
    public void serializeMessage(final Notification message, final ByteBuf bytes) {
        Preconditions.checkArgument(message instanceof RouteRefresh, "Message is not of type RouteRefresh.");
        final RouteRefresh msg = (RouteRefresh) message;

        final ByteBuf msgBuf = Unpooled.buffer(TRIPLET_BYTE_SIZE);
        final Integer afival = this.afiReg.numberForClass(msg.getAfi());
        Preconditions.checkArgument(afival != null, "Unhandled address family " + msg.getAfi());
        msgBuf.writeShort(afival);

        msgBuf.writeZero(RESERVED);

        final Integer safival = this.safiReg.numberForClass(msg.getSafi());
        Preconditions.checkArgument(safival != null, "Unhandled subsequent address family " + msg.getSafi());
        msgBuf.writeByte(safival);

        LOG.trace("RouteRefresh message serialized to: {}", ByteBufUtil.hexDump(msgBuf));
        MessageUtil.formatMessage(TYPE, msgBuf, bytes);
    }

    /**
     * Parses BGP Route Refresh message to bytes.
     *
     * @param body ByteBuf to be parsed
     * @param messageLength the length of the message
     * @return {@link RouteRefresh} which represents BGP notification message
     * @throws BGPDocumentedException if parsing goes wrong
     */
    @Override
    public RouteRefresh parseMessageBody(final ByteBuf body, final int messageLength) throws BGPDocumentedException {
        Preconditions.checkArgument(body != null, "Body buffer cannot be null.");
        if (body.readableBytes() < TRIPLET_BYTE_SIZE) {
            throw BGPDocumentedException.badMessageLength("RouteRefresh message is too small.", messageLength);
        }
        final int afiVal = body.readUnsignedShort();
        final Class<? extends AddressFamily> afi = this.afiReg.classForFamily(afiVal);
        if (afi == null) {
            throw new BGPDocumentedException("Address Family Identifier: "+afiVal+" is not supported.", BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);
        }
        body.readBytes(RESERVED);
        final int safiVal = body.readUnsignedByte();
        final Class<? extends SubsequentAddressFamily> safi = this.safiReg.classForFamily(safiVal);
        if (safi == null) {
            throw new BGPDocumentedException("Subsequent Address Family Identifier: "+safiVal+" is not supported.", BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);
        }
        return new RouteRefreshBuilder().setAfi(afi).setSafi(safi).build();
    }

}
