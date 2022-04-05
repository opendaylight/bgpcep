/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.ParameterLengthOverflowException;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
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

    // Optional Parameter Type for Extended Optional Parameters Length
    private static final int OPT_PARAM_EXT_PARAM = 255;

    private static final int MIN_MSG_LENGTH = VERSION_SIZE + AS_SIZE
            + HOLD_TIME_SIZE + BGP_ID_SIZE + OPT_PARAM_LENGTH_SIZE;

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
    public void serializeMessage(final Notification<?> msg, final ByteBuf bytes) {
        checkArgument(msg instanceof Open, "Message needs to be of type Open, not %s", msg);
        final Open open = (Open) msg;
        final ByteBuf msgBody = Unpooled.buffer()
                .writeByte(BGP_VERSION);

        // When our AS number does not fit into two bytes, we report it as AS_TRANS
        int openAS = open.getMyAsNumber().toJava();
        if (openAS > Values.UNSIGNED_SHORT_MAX_VALUE) {
            openAS = AS_TRANS;
        }
        msgBody
            .writeShort(openAS)
            .writeShort(open.getHoldTimer().toJava())
            .writeBytes(Ipv4Util.bytesForAddress(open.getBgpIdentifier()));

        serializeParameters(open.getBgpParameters(), msgBody);

        MessageUtil.formatMessage(TYPE, msgBody, bytes);
    }

    private void serializeParameters(final List<BgpParameters> params, final ByteBuf msgBody) {
        if (params == null || params.isEmpty()) {
            msgBody.writeByte(0);
            return;
        }

        final ByteBuf normal = normalSerializeParameters(params);
        if (normal != null) {
            final int length = normal.writerIndex();
            verify(length <= Values.UNSIGNED_BYTE_MAX_VALUE);
            msgBody.writeByte(length);
            msgBody.writeBytes(normal);
            return;
        }

        final ByteBuf buffer = Unpooled.buffer();
        for (final BgpParameters param : params) {
            final Optional<ParameterSerializer> optSer = reg.findSerializer(param);
            if (optSer.isPresent()) {
                optSer.get().serializeExtendedParameter(param, buffer);
            } else {
                LOG.debug("Ignoring unregistered parameter {}", param);
            }
        }

        final int length = buffer.writerIndex();
        checkState(length <= Values.UNSIGNED_SHORT_MAX_VALUE);

        // The non-extended Optional Parameters Length field MUST be set to 255
        msgBody.writeByte(Values.UNSIGNED_BYTE_MAX_VALUE);
        // The subsequent one-octet field, that in the non-extended format would
        // be the first Optional Parameter Type field, MUST be set to 255
        msgBody.writeByte(OPT_PARAM_EXT_PARAM);
        // Extended Optional Parameters Length
        msgBody.writeShort(length);
        msgBody.writeBytes(buffer);
    }

    private ByteBuf normalSerializeParameters(final List<BgpParameters> params) {
        final ByteBuf buffer = Unpooled.buffer();
        for (final BgpParameters param : params) {
            final Optional<ParameterSerializer> optSer = reg.findSerializer(param);
            if (optSer.isPresent()) {
                try {
                    optSer.get().serializeParameter(param, buffer);
                } catch (ParameterLengthOverflowException e) {
                    LOG.debug("Forcing extended parameter serialization", e);
                    return null;
                }
            } else {
                LOG.debug("Ingnoring unregistered parameter {}", param);
            }
        }

        final int length = buffer.writerIndex();
        if (length > Values.UNSIGNED_BYTE_MAX_VALUE) {
            LOG.debug("Final parameter size is {}, forcing extended serialization", length);
            return null;
        }

        return buffer;
    }

    /**
     * Parses given byte array to BGP Open message.
     *
     * @param body byte array representing BGP Open message, without header
     * @param messageLength the length of the message
     * @return {@link Open} BGP Open Message
     * @throws BGPDocumentedException if the parsing was unsuccessful
     */
    @Override
    public Open parseMessageBody(final ByteBuf body, final int messageLength,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
        checkArgument(body != null, "Buffer cannot be null.");

        if (body.readableBytes() < MIN_MSG_LENGTH) {
            throw BGPDocumentedException.badMessageLength("Open message too small.", messageLength);
        }
        final int version = body.readUnsignedByte();
        if (version != BGP_VERSION) {
            throw new BGPDocumentedException("BGP Protocol version " + version + " not supported.",
                    BGPError.VERSION_NOT_SUPPORTED);
        }
        final AsNumber as = new AsNumber(Uint32.valueOf(body.readUnsignedShort()));
        final Uint16 holdTime = ByteBufUtils.readUint16(body);
        if (Uint16.ONE.equals(holdTime) || Uint16.TWO.equals(holdTime)) {
            throw new BGPDocumentedException("Hold time value not acceptable.", BGPError.HOLD_TIME_NOT_ACC);
        }
        final Ipv4AddressNoZone bgpId;
        try {
            bgpId = Ipv4Util.addressForByteBuf(body);
        } catch (final IllegalArgumentException e) {
            throw new BGPDocumentedException("BGP Identifier is not a valid IPv4 Address", BGPError.BAD_BGP_ID, e);
        }
        final int optLength = body.readUnsignedByte();
        final List<BgpParameters> optParams = parseParameters(body.slice(), optLength);
        LOG.debug("BGP Open message was parsed: AS = {}, holdTimer = {}, bgpId = {}, optParams = {}", as,
                holdTime, bgpId, optParams);
        return new OpenBuilder().setMyAsNumber(Uint16.valueOf(as.getValue())).setHoldTimer(holdTime)
                .setBgpIdentifier(bgpId).setBgpParameters(optParams).build();
    }

    private List<BgpParameters> parseParameters(final ByteBuf buffer, final int length) throws BGPDocumentedException {
        if (length == 0) {
            return ImmutableList.of();
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Started parsing of BGP parameter: {} length {}", ByteBufUtil.hexDump(buffer), length);
        }

        final int realLength;
        final OptionalInt extendedLength = extractExtendedLength(buffer, length);
        if (extendedLength.isPresent()) {
            realLength = extendedLength.getAsInt();
            if (realLength < Values.UNSIGNED_BYTE_MAX_VALUE) {
                LOG.debug("Peer used Extended Optional Parameters Length to encode length {}", realLength);
            }
        } else {
            realLength = length;
        }

        // We have determined the real length, we can trim the buffer now
        if (buffer.readableBytes() > realLength) {
            buffer.writerIndex(buffer.readerIndex() + realLength);
            LOG.trace("Truncated BGP parameter buffer to length {}: {}", realLength, ByteBufUtil.hexDump(buffer));
        }

        final int lengthSize = extendedLength.isPresent() ? 1 : 2;
        final List<BgpParameters> params = new ArrayList<>();
        while (buffer.isReadable()) {
            final int paramType = buffer.readUnsignedByte();
            final Optional<ParameterParser> parser = reg.findParser(paramType);
            if (!parser.isPresent()) {
                throw new BGPDocumentedException("Parameter " + paramType + " not supported",
                    BGPError.OPT_PARAM_NOT_SUPPORTED);
            }
            if (buffer.readableBytes() <= lengthSize) {
                throw new BGPDocumentedException("Malformed parameter encountered (" + buffer.readableBytes()
                        + " bytes left)", BGPError.UNSPECIFIC_OPEN_ERROR);
            }
            final int paramLength = extendedLength.isPresent() ? buffer.readUnsignedShort() : buffer.readUnsignedByte();
            final ByteBuf paramBody = buffer.readSlice(paramLength);

            final BgpParameters param;
            try {
                param = parser.get().parseParameter(paramBody);
            } catch (final BGPParsingException e) {
                throw new BGPDocumentedException("Optional parameter not parsed", BGPError.UNSPECIFIC_OPEN_ERROR, e);
            }

            params.add(verifyNotNull(param));
        }

        LOG.trace("Parsed BGP parameters: {}", params);
        return params;
    }

    private static OptionalInt extractExtendedLength(final ByteBuf buffer, final int length)
            throws BGPDocumentedException {
        final int type = buffer.markReaderIndex().readUnsignedByte();
        if (type != OPT_PARAM_EXT_PARAM) {
            // Not extended length
            buffer.resetReaderIndex();
            return OptionalInt.empty();
        }
        if (length != Values.UNSIGNED_BYTE_MAX_VALUE) {
            LOG.debug("Peer uses Extended Optional Parameters Length, but indicated RFC4271 length as {}", length);
        }
        if (length < 3) {
            throw new BGPDocumentedException("Malformed Extended Length parameter encountered ("
                    + (length - 1) + " bytes left)", BGPError.UNSPECIFIC_OPEN_ERROR);
        }
        final int avail = buffer.readableBytes();
        if (avail < 2) {
            throw new BGPDocumentedException("Buffer underrun: require 2 bytes, only " + avail + " bytes left",
                BGPError.UNSPECIFIC_OPEN_ERROR);
        }

        return OptionalInt.of(buffer.readUnsignedShort());
    }
}
