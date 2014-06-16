/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for BGP Open message.
 */
public final class BGPOpenMessageParser implements MessageParser, MessageSerializer {

    public static final int TYPE = 1;

    private static final Logger LOG = LoggerFactory.getLogger(BGPOpenMessageParser.class);

    private static final int VERSION_SIZE = 1;
    private static final int AS_SIZE = 2;
    private static final int HOLD_TIME_SIZE = 2;
    private static final int BGP_ID_SIZE = 4;
    private static final int OPT_PARAM_LENGTH_SIZE = 1;

    private static final int MIN_MSG_LENGTH = VERSION_SIZE + AS_SIZE + HOLD_TIME_SIZE + BGP_ID_SIZE + OPT_PARAM_LENGTH_SIZE;

    private static final int BGP_VERSION = 4;

    private static final int AS_TRANS = 2345;

    private final ParameterRegistry reg;

    public BGPOpenMessageParser(final ParameterRegistry reg) {
        this.reg = Preconditions.checkNotNull(reg);
    }

    /**
     * Serializes given BGP Open message to byte array, without the header.
     *
     * @param msg BGP Open message to be serialized.
     * @return BGP Open message converted to byte array
     */
    @Override
    public void serializeMessage(final Notification msg, ByteBuf bytes) {
        if (msg == null) {
            throw new IllegalArgumentException("BGPOpen message cannot be null");
        }
        LOG.trace("Started serializing open message: {}", msg);
        final Open open = (Open) msg;

        final Map<byte[], Integer> optParams = Maps.newHashMap();

        int optParamsLength = 0;

        if (open.getBgpParameters() != null) {
            for (final BgpParameters param : open.getBgpParameters()) {
                final byte[] p = this.reg.serializeParameter(param);
                if (p != null) {
                    optParams.put(p, p.length);
                    optParamsLength += p.length;
                }
            }
        }
        final byte[] msgBody = new byte[MIN_MSG_LENGTH + optParamsLength];

        int offset = 0;

        msgBody[offset] = UnsignedBytes.checkedCast(BGP_VERSION);
        offset += VERSION_SIZE;

        // When our AS number does not fit into two bytes, we report it as AS_TRANS
        int openAS = open.getMyAsNumber();
        if (openAS > Values.UNSIGNED_SHORT_MAX_VALUE) {
            openAS = AS_TRANS;
        }
        System.arraycopy(ByteArray.longToBytes(openAS, AS_SIZE), 0, msgBody, offset, AS_SIZE);
        offset += AS_SIZE;

        System.arraycopy(ByteArray.intToBytes(open.getHoldTimer(), HOLD_TIME_SIZE), 0, msgBody, offset, HOLD_TIME_SIZE);
        offset += HOLD_TIME_SIZE;

        System.arraycopy(Ipv4Util.bytesForAddress(open.getBgpIdentifier()), 0, msgBody, offset, BGP_ID_SIZE);
        offset += BGP_ID_SIZE;

        msgBody[offset] = UnsignedBytes.checkedCast(optParamsLength);

        int index = MIN_MSG_LENGTH;
        if (optParams != null) {
            for (final Entry<byte[], Integer> entry : optParams.entrySet()) {
                System.arraycopy(entry.getKey(), 0, msgBody, index, entry.getValue());
                index += entry.getValue();
            }
        }
        bytes.writeBytes(MessageUtil.formatMessage(TYPE, msgBody));
        LOG.trace("Open message serialized to: {}", ByteBufUtil.hexDump(bytes));
    }

    /**
     * Parses given byte array to BGP Open message
     *
     * @param body byte array representing BGP Open message, without header
     * @return BGP Open Message
     * @throws BGPDocumentedException if the parsing was unsuccessful
     */
    @Override
    public Open parseMessageBody(final ByteBuf body, final int messageLength) throws BGPDocumentedException {
        if (body == null) {
            throw new IllegalArgumentException("Byte array cannot be null.");
        }
        LOG.trace("Started parsing of open message: {}", Arrays.toString(ByteArray.getAllBytes(body)));

        if (body.readableBytes() < MIN_MSG_LENGTH) {
            throw BGPDocumentedException.badMessageLength("Open message too small.", messageLength);
        }
        int version = UnsignedBytes.toInt(body.readByte());
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
            bgpId = Ipv4Util.addressForBytes(ByteArray.readBytes(body, BGP_ID_SIZE));
        } catch (final IllegalArgumentException e) {
            throw new BGPDocumentedException("BGP Identifier is not a valid IPv4 Address", BGPError.BAD_BGP_ID, e);
        }
        final int optLength = UnsignedBytes.toInt(body.readByte());

        final List<BgpParameters> optParams = Lists.newArrayList();
        if (optLength > 0) {
            fillParams(body.slice(body.readerIndex(), optLength), optParams);
        }
        LOG.debug("BGP Open message was parsed: AS = {}, holdTimer = {}, bgpId = {}, optParams = {}", as, holdTime, bgpId, optParams);
        return new OpenBuilder().setMyAsNumber(as.getValue().intValue()).setHoldTimer(holdTime).setBgpIdentifier(bgpId).setBgpParameters(
                optParams).build();
    }

    private void fillParams(final ByteBuf buffer, final List<BgpParameters> params) throws BGPDocumentedException {
        Preconditions.checkArgument(buffer != null && buffer.readableBytes() != 0, "Byte array cannot be null or empty.");
        LOG.trace("Started parsing of BGP parameter: {}", Arrays.toString(ByteArray.getAllBytes(buffer)));
        while (buffer.readableBytes() != 0) {
            if (buffer.readableBytes() <= 2) {
                throw new BGPDocumentedException("Malformed parameter encountered (" + buffer.readableBytes() + " bytes left)", BGPError.OPT_PARAM_NOT_SUPPORTED);
            }
            final int paramType = UnsignedBytes.toInt(buffer.readByte());
            final int paramLength = UnsignedBytes.toInt(buffer.readByte());
            final ByteBuf paramBody = buffer.slice(buffer.readerIndex(), paramLength);

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
            buffer.skipBytes(paramLength);
        }
        LOG.trace("Parsed BGP parameters: {}", Arrays.toString(params.toArray()));
    }
}
