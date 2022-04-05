/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefreshBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPRouteRefreshMessageParser implements MessageParser, MessageSerializer {
    // https://tools.ietf.org/html/rfc2918#section-3

    private static final Logger LOG = LoggerFactory.getLogger(BGPRouteRefreshMessageParser.class);

    public static final int TYPE = 5;
    private static final int TRIPLET_BYTE_SIZE = 4;
    private static final String ARGUMENT_ERROR = "Message is not of type RouteRefresh.";

    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public BGPRouteRefreshMessageParser(final AddressFamilyRegistry afiReg,
            final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = requireNonNull(afiReg);
        this.safiReg = requireNonNull(safiReg);
    }

    /**
     * Serializes BGP Route Refresh message.
     *
     * @param message to be serialized
     * @param bytes ByteBuf where the message will be serialized
     */
    @Override
    public void serializeMessage(final Notification<?> message, final ByteBuf bytes) {
        checkArgument(message instanceof RouteRefresh, ARGUMENT_ERROR);
        final RouteRefresh msg = (RouteRefresh) message;

        final ByteBuf msgBuf = Unpooled.buffer(TRIPLET_BYTE_SIZE);
        MultiprotocolCapabilitiesUtil.serializeMPAfiSafi(afiReg, safiReg,
                msg.getAfi(), msg.getSafi(), msgBuf);

        if (LOG.isTraceEnabled()) {
            LOG.trace("RouteRefresh message serialized to: {}", ByteBufUtil.hexDump(msgBuf));
        }
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
    public RouteRefresh parseMessageBody(final ByteBuf body, final int messageLength,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
        checkArgument(body != null, "Body buffer cannot be null.");
        if (body.readableBytes() < TRIPLET_BYTE_SIZE) {
            throw BGPDocumentedException.badMessageLength("RouteRefresh message is too small.", messageLength);
        }
        return new RouteRefreshBuilder(MultiprotocolCapabilitiesUtil.parseMPAfiSafi(body, afiReg, safiReg)
            .orElseThrow(() -> new BGPDocumentedException("Unsupported afi/safi in Route Refresh message.",
                BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED))
            ).build();
    }
}
