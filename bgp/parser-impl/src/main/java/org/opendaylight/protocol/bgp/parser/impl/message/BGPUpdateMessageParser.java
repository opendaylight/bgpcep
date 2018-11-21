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
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.NextHopAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.ParsedAttributes;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.TreatAsWithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LENGTH fields, that denote the length of the fields with variable length, have fixed SIZE.
 *
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.3">BGP-4 Update Message Format</a>
 */
public final class BGPUpdateMessageParser implements MessageParser, MessageSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(BGPUpdateMessageParser.class);

    public static final int TYPE = 2;

    private static final int WITHDRAWN_ROUTES_LENGTH_SIZE = 2;

    private static final int TOTAL_PATH_ATTR_LENGTH_SIZE = 2;

    private final AttributeRegistry reg;

    public BGPUpdateMessageParser(final AttributeRegistry reg) {
        this.reg = requireNonNull(reg);
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf bytes) {
        Preconditions.checkArgument(message instanceof Update, "Message needs to be of type Update");
        final Update update = (Update) message;

        final ByteBuf messageBody = Unpooled.buffer();
        final List<WithdrawnRoutes> withdrawnRoutes = update.getWithdrawnRoutes();
        if (withdrawnRoutes != null) {
            final ByteBuf withdrawnRoutesBuf = Unpooled.buffer();
            withdrawnRoutes.forEach(withdrawnRoute -> writePathIdPrefix(withdrawnRoutesBuf, withdrawnRoute.getPathId(),
                    withdrawnRoute.getPrefix()));
            messageBody.writeShort(withdrawnRoutesBuf.writerIndex());
            messageBody.writeBytes(withdrawnRoutesBuf);
        } else {
            messageBody.writeZero(WITHDRAWN_ROUTES_LENGTH_SIZE);
        }
        if (update.getAttributes() != null) {
            final ByteBuf pathAttributesBuf = Unpooled.buffer();
            this.reg.serializeAttribute(update.getAttributes(), pathAttributesBuf);
            messageBody.writeShort(pathAttributesBuf.writerIndex());
            messageBody.writeBytes(pathAttributesBuf);
        } else {
            messageBody.writeZero(TOTAL_PATH_ATTR_LENGTH_SIZE);
        }
        final List<Nlri> nlris = update.getNlri();
        if (nlris != null) {
            nlris.forEach(nlri -> writePathIdPrefix(messageBody, nlri.getPathId(), nlri.getPrefix()));
        }
        MessageUtil.formatMessage(TYPE, messageBody, bytes);
    }

    private static void writePathIdPrefix(final ByteBuf byteBuf, final PathId pathId, final Ipv4Prefix ipv4Prefix) {
        PathIdUtil.writePathId(pathId, byteBuf);
        ByteBufWriteUtil.writeMinimalPrefix(ipv4Prefix, byteBuf);
    }

    /**
     * Parse Update message from buffer.
     * Calls {@link #checkMandatoryAttributesPresence(Update)} to check for presence of mandatory attributes.
     *
     * @param buffer Encoded BGP message in ByteBuf
     * @param messageLength Length of the BGP message
     * @param constraint Peer specific constraints
     * @return Parsed Update message body
     */
    @Override
    public Update parseMessageBody(final ByteBuf buffer, final int messageLength,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
                "Buffer cannot be null or empty.");

        final UpdateBuilder builder = new UpdateBuilder();
        final boolean isMultiPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
                new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        final RevisedErrorHandling errorHandling = RevisedErrorHandling.from(constraint);

        final int withdrawnRoutesLength = buffer.readUnsignedShort();
        if (withdrawnRoutesLength > 0) {
            final List<WithdrawnRoutes> withdrawnRoutes = new ArrayList<>();
            final ByteBuf withdrawnRoutesBuffer = buffer.readBytes(withdrawnRoutesLength);
            while (withdrawnRoutesBuffer.isReadable()) {
                final WithdrawnRoutesBuilder withdrawnRoutesBuilder = new WithdrawnRoutesBuilder();
                if (isMultiPathSupported) {
                    withdrawnRoutesBuilder.setPathId(PathIdUtil.readPathId(withdrawnRoutesBuffer));
                }
                withdrawnRoutesBuilder.setPrefix(readPrefix(withdrawnRoutesBuffer, errorHandling));
                withdrawnRoutes.add(withdrawnRoutesBuilder.build());
            }
            withdrawnRoutesBuffer.release();
            builder.setWithdrawnRoutes(withdrawnRoutes);
        }
        final int totalPathAttrLength = buffer.readUnsignedShort();
        if (withdrawnRoutesLength == 0 && totalPathAttrLength == 0) {
            return builder.build();
        }

        final Optional<BGPTreatAsWithdrawException> withdrawCause;
        if (totalPathAttrLength > 0) {
            final ParsedAttributes attributes;
            try {
                attributes = this.reg.parseAttributes(buffer.readSlice(totalPathAttrLength), constraint);
            } catch (final RuntimeException | BGPParsingException e) {
                // Catch everything else and turn it into a BGPDocumentedException
                throw new BGPDocumentedException("Could not parse BGP attributes.", BGPError.MALFORMED_ATTR_LIST, e);
            }

            builder.setAttributes(attributes.getAttributes());
            withdrawCause = attributes.getWithdrawCause();
        } else {
            withdrawCause = Optional.empty();
        }

        final List<Nlri> nlri = new ArrayList<>();
        while (buffer.isReadable()) {
            final NlriBuilder nlriBuilder = new NlriBuilder();
            if (isMultiPathSupported) {
                nlriBuilder.setPathId(PathIdUtil.readPathId(buffer));
            }
            nlriBuilder.setPrefix(readPrefix(buffer, errorHandling));
            nlri.add(nlriBuilder.build());
        }
        if (!nlri.isEmpty()) {
            builder.setNlri(nlri);
        }

        if (withdrawCause.isPresent() && errorHandling != RevisedErrorHandling.NONE) {
            LOG.debug("Converting BGP Update message to withdraw");
            updateToWithdraw(builder);
            throw withdrawCause.get().toDocumentedException();
        }

        Update msg = builder.build();
        checkMandatoryAttributesPresence(msg);

        LOG.debug("BGP Update message was parsed {}.", msg);
        return msg;
    }

    private static Ipv4Prefix readPrefix(final ByteBuf buf, final RevisedErrorHandling errorHandling)
            throws BGPDocumentedException {
        final int prefixLength = UnsignedBytes.toInt(buf.readByte());
        if (errorHandling != RevisedErrorHandling.NONE) {
            // https://tools.ietf.org/html/rfc7606#section-5.3
            if (prefixLength > 32) {
                throw new BGPDocumentedException("Withdrawn route length " + prefixLength + " exceeds 32 bytes",
                    BGPError.ATTR_LENGTH_ERROR);
            }
            if (prefixLength > buf.readableBytes() * 8) {
                throw new BGPDocumentedException(
                    "Withdrawn route length " + prefixLength + " exceeds unconsumed field space",
                    BGPError.ATTR_LENGTH_ERROR);
            }
        }

        return Ipv4Util.prefixForByteBuf(buf, prefixLength);
    }

    /**
     * Check for presence of well known mandatory path attributes
     * ORIGIN, AS_PATH and NEXT_HOP in Update message
     *
     * @param message Update message
     * @throws BGPDocumentedException
     */
    private static void checkMandatoryAttributesPresence(final Update message) throws BGPDocumentedException {
        requireNonNull(message, "Update message cannot be null");

        final Attributes attrs = message.getAttributes();

        if (message.getNlri() != null) {
            if (attrs == null || attrs.getCNextHop() == null) {
                throw new BGPDocumentedException(BGPError.MANDATORY_ATTR_MISSING_MSG + "NEXT_HOP",
                        BGPError.WELL_KNOWN_ATTR_MISSING,
                        new byte[] { NextHopAttributeParser.TYPE });
            }
        }

        if (MessageUtil.isAnyNlriPresent(message)) {
            if (attrs == null || attrs.getOrigin() == null) {
                throw new BGPDocumentedException(BGPError.MANDATORY_ATTR_MISSING_MSG + "ORIGIN",
                        BGPError.WELL_KNOWN_ATTR_MISSING,
                        new byte[] { OriginAttributeParser.TYPE });
            }

            if (attrs.getAsPath() == null) {
                throw new BGPDocumentedException(BGPError.MANDATORY_ATTR_MISSING_MSG + "AS_PATH",
                        BGPError.WELL_KNOWN_ATTR_MISSING,
                        new byte[] { AsPathAttributeParser.TYPE });
            }
        }
    }

    private static void updateToWithdraw(final UpdateBuilder builder) {
        final List<Nlri> nlris = builder.getNlri();
        if (nlris != null && !nlris.isEmpty()) {
            final List<WithdrawnRoutes> withdrawnRoutes = builder.getWithdrawnRoutes();
            for (Nlri nlri : nlris) {
                withdrawnRoutes.add(new WithdrawnRoutesBuilder()
                        .setPathId(nlri.getPathId())
                        .setPrefix(nlri.getPrefix())
                        .build());
            }
            builder.setWithdrawnRoutes(withdrawnRoutes);
        }
        reachToUnreach(builder);
    }

    private static void reachToUnreach( final UpdateBuilder builder) {
        final Attributes attributes = builder.getAttributes();
        if (attributes != null) {
            final Attributes1 reachAttr = attributes.augmentation(Attributes1.class);
            if (reachAttr != null) {
                final MpReachNlri mpReachNlri = reachAttr.getMpReachNlri();
                if (mpReachNlri != null) {
                    final DestinationType destinationType = mpReachNlri.getAdvertizedRoutes().getDestinationType();
                    final MpUnreachNlriBuilder unreachNlri;
                    final Attributes2 unreachAttr = attributes.augmentation(Attributes2.class);
                    if (unreachAttr != null && unreachAttr.getMpUnreachNlri() != null) {
                        unreachNlri = new MpUnreachNlriBuilder(unreachAttr.getMpUnreachNlri());
                    } else {
                        unreachNlri = new MpUnreachNlriBuilder()
                                .setAfi(mpReachNlri.getAfi())
                                .setSafi(mpReachNlri.getSafi());
                    }
                    unreachNlri.setTreatAsWithdrawnRoutes(new TreatAsWithdrawnRoutesBuilder()
                            .setDestinationType(destinationType)
                            .build())
                            .build();
                    final Attributes newAttributes = new AttributesBuilder(attributes)
                            .removeAugmentation(Attributes1.class)
                            .addAugmentation(Attributes2.class, new Attributes2Builder()
                                    .setMpUnreachNlri(unreachNlri.build())
                                    .build())
                            .build();
                    builder.setAttributes(newAttributes);
                }
            }
        }
    }
}