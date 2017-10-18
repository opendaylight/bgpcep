/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.BgpParameters;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for BGP Open message.
 */
public final class BGPOpenMessageParser implements MessageParser, MessageSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(BGPOpenMessageParser.class);

    public static final int TYPE = 1;

    private static final int VERSION_SIZE = 1;
    private static final int AS_SIZE = 2;
    private static final int HOLD_TIME_SIZE = 2;
    private static final int BGP_ID_SIZE = 4;
    private static final int OPT_PARAM_LENGTH_SIZE = 1;

    private static final int MIN_MSG_LENGTH = VERSION_SIZE + AS_SIZE + HOLD_TIME_SIZE + BGP_ID_SIZE + OPT_PARAM_LENGTH_SIZE;

    private static final int BGP_VERSION = 4;

    public static final int AS_TRANS = 23456;

    private final ParameterRegistry reg;

    public BGPOpenMessageParser(final ParameterRegistry reg) {
        this.reg = requireNonNull(reg);
    }

    /**
     * Serializes given BGP Open message to byte array, without the header.
     *
     * @param msg BGP Open message to be serialized.
     * @param bytes ByteBuf where the message will be serialized
     */
    @Override
    public void serializeMessage(final Notification msg, final ByteBuf bytes) {
        Preconditions.checkArgument(msg instanceof Open, "Message needs to be of type Open");
        final Open open = (Open) msg;
        final ByteBuf msgBody = Unpooled.buffer();

        msgBody.writeByte(BGP_VERSION);

        // When our AS number does not fit into two bytes, we report it as AS_TRANS
        int openAS = open.getMyAsNumber();
        if (openAS > Values.UNSIGNED_SHORT_MAX_VALUE) {
            openAS = AS_TRANS;
        }
        msgBody.writeShort(openAS);
        msgBody.writeShort(open.getHoldTimer());
        msgBody.writeBytes(Ipv4Util.bytesForAddress(open.getBgpIdentifier()));

        final ByteBuf paramsBuffer = Unpooled.buffer();
        if (open.getBgpParameters() != null) {
            for (final BgpParameters param : open.getBgpParameters()) {
                this.reg.serializeParameter(param, paramsBuffer);
            }
        }
        msgBody.writeByte(paramsBuffer.writerIndex());
        msgBody.writeBytes(paramsBuffer);

        MessageUtil.formatMessage(TYPE, msgBody, bytes);
    }

    /**
     * Parses given byte array to BGP Open message
     *
     * @param body byte array representing BGP Open message, without header
     * @param messageLength the length of the message
     * @return {@link Open} BGP Open Message
     * @throws BGPDocumentedException if the parsing was unsuccessful
     */
    @Override
    public Open parseMessageBody(final ByteBuf body, final int messageLength) throws BGPDocumentedException {
        Preconditions.checkArgument(body != null, "Buffer cannot be null.");

        if (body.readableBytes() < MIN_MSG_LENGTH) {
            throw BGPDocumentedException.badMessageLength("Open message too small.", messageLength);
        }
        final int version = body.readUnsignedByte();
        if (version != BGP_VERSION) {
            throw new BGPDocumentedException("BGP Protocol version " + version + " not supported.", BGPError.VERSION_NOT_SUPPORTED);
        }
        final AsNumber as = new AsNumber((long) body.readUnsignedShort());
        final int holdTime = body.readUnsignedShort();
        if (holdTime == 1 || holdTime == 2) {
            throw new BGPDocumentedException("Hold time value not acceptable.", BGPError.HOLD_TIME_NOT_ACC);
        }
        Ipv4Address bgpId = null;
        try {
            bgpId = Ipv4Util.addressForByteBuf(body);
        } catch (final IllegalArgumentException e) {
            throw new BGPDocumentedException("BGP Identifier is not a valid IPv4 Address", BGPError.BAD_BGP_ID, e);
        }
        final int optLength = body.readUnsignedByte();

        final List<BgpParameters> optParams = new ArrayList<>();
        if (optLength > 0) {
            fillParams(body.slice(), optParams);
        }
        LOG.debug("BGP Open message was parsed: AS = {}, holdTimer = {}, bgpId = {}, optParams = {}", as, holdTime, bgpId, optParams);
        return new OpenBuilder().setMyAsNumber(as.getValue().intValue()).setHoldTimer(holdTime).setBgpIdentifier(bgpId).setBgpParameters(
            optParams).build();
    }

    private void fillParams(final ByteBuf buffer, final List<BgpParameters> params) throws BGPDocumentedException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "BUffer cannot be null or empty.");
        if (LOG.isTraceEnabled()) {
            LOG.trace("Started parsing of BGP parameter: {}", ByteBufUtil.hexDump(buffer));
        }
        while (buffer.isReadable()) {
            if (buffer.readableBytes() <= 2) {
                throw new BGPDocumentedException("Malformed parameter encountered (" + buffer.readableBytes() + " bytes left)", BGPError.OPT_PARAM_NOT_SUPPORTED);
            }
            final int paramType = buffer.readUnsignedByte();
            final int paramLength = buffer.readUnsignedByte();
            final ByteBuf paramBody = buffer.readSlice(paramLength);

            final BgpParameters param;
            try {
                param = this.reg.parseParameter(paramType, paramBody);
            } catch (final BGPParsingException e) {
                throw new BGPDocumentedException("Optional parameter not parsed", BGPError.UNSPECIFIC_OPEN_ERROR, e);
            }
            if (param != null) {
                params.add(param);
            } else {
                LOG.debug("Ignoring BGP Parameter type: {}", paramType);
            }
        }
        LOG.trace("Parsed BGP parameters: {}", params);
    }
}
