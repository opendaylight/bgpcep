/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
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
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParsedAttributes;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
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

    private final AttributeRegistry attrReg;
    private final NlriRegistry nlriReg;

    public BGPUpdateMessageParser(final AttributeRegistry attrReg, final NlriRegistry nlriReg) {
        this.attrReg = requireNonNull(attrReg);
        this.nlriReg = requireNonNull(nlriReg);
    }

    @Override
    public void serializeMessage(final Notification<?> message, final ByteBuf bytes) {
        checkArgument(message instanceof Update, "Message needs to be of type Update");
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
            messageBody.writeShort(0);
        }
        if (update.getAttributes() != null) {
            final ByteBuf pathAttributesBuf = Unpooled.buffer();
            attrReg.serializeAttribute(update.getAttributes(), pathAttributesBuf);
            messageBody.writeShort(pathAttributesBuf.writerIndex());
            messageBody.writeBytes(pathAttributesBuf);
        } else {
            messageBody.writeShort(0);
        }
        final List<Nlri> nlris = update.getNlri();
        if (nlris != null) {
            nlris.forEach(nlri -> writePathIdPrefix(messageBody, nlri.getPathId(), nlri.getPrefix()));
        }
        MessageUtil.formatMessage(TYPE, messageBody, bytes);
    }

    private static void writePathIdPrefix(final ByteBuf byteBuf, final PathId pathId, final Ipv4Prefix ipv4Prefix) {
        PathIdUtil.writePathId(pathId, byteBuf);
        Ipv4Util.writeMinimalPrefix(ipv4Prefix, byteBuf);
    }

    /**
     * Parse Update message from buffer. Calls {@link #checkMandatoryAttributesPresence(Update, RevisedErrorHandling)}
     * to check for presence of mandatory attributes.
     *
     * @param buffer Encoded BGP message in ByteBuf
     * @param messageLength Length of the BGP message
     * @param constraint Peer specific constraints
     * @return Parsed Update message body
     */
    @Override
    public Update parseMessageBody(final ByteBuf buffer, final int messageLength,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
        checkArgument(buffer != null && buffer.isReadable(),"Buffer cannot be null or empty.");

        final UpdateBuilder builder = new UpdateBuilder();
        final boolean isMultiPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
                new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE));
        final RevisedErrorHandling errorHandling = RevisedErrorHandling.from(constraint);

        final int withdrawnRoutesLength = buffer.readUnsignedShort();
        if (withdrawnRoutesLength > 0) {
            final List<WithdrawnRoutes> withdrawnRoutes = new ArrayList<>();
            final ByteBuf withdrawnRoutesBuffer = buffer.readSlice(withdrawnRoutesLength);
            while (withdrawnRoutesBuffer.isReadable()) {
                final WithdrawnRoutesBuilder withdrawnRoutesBuilder = new WithdrawnRoutesBuilder();
                if (isMultiPathSupported) {
                    withdrawnRoutesBuilder.setPathId(PathIdUtil.readPathId(withdrawnRoutesBuffer));
                }
                withdrawnRoutesBuilder.setPrefix(readPrefix(withdrawnRoutesBuffer, errorHandling, "Withdrawn Routes"));
                withdrawnRoutes.add(withdrawnRoutesBuilder.build());
            }
            builder.setWithdrawnRoutes(withdrawnRoutes);
        }
        final int totalPathAttrLength = buffer.readUnsignedShort();
        if (withdrawnRoutesLength == 0 && totalPathAttrLength == 0) {
            return builder.build();
        }

        Optional<BGPTreatAsWithdrawException> withdrawCauseOpt;
        if (totalPathAttrLength > 0) {
            final ParsedAttributes attributes = parseAttributes(buffer, totalPathAttrLength, constraint);
            builder.setAttributes(attributes.getAttributes());
            withdrawCauseOpt = attributes.getWithdrawCause();
        } else {
            withdrawCauseOpt = Optional.empty();
        }

        final List<Nlri> nlri = new ArrayList<>();
        while (buffer.isReadable()) {
            final NlriBuilder nlriBuilder = new NlriBuilder();
            if (isMultiPathSupported) {
                nlriBuilder.setPathId(PathIdUtil.readPathId(buffer));
            }
            nlriBuilder.setPrefix(readPrefix(buffer, errorHandling, "NLRI"));
            nlri.add(nlriBuilder.build());
        }
        if (!nlri.isEmpty()) {
            builder.setNlri(nlri);
        }

        try {
            checkMandatoryAttributesPresence(builder.build(), errorHandling);
        } catch (BGPTreatAsWithdrawException e) {
            LOG.debug("Well-known mandatory attributes missing", e);
            if (withdrawCauseOpt.isPresent()) {
                final BGPTreatAsWithdrawException exception = withdrawCauseOpt.get();
                exception.addSuppressed(e);
                withdrawCauseOpt = Optional.of(exception);
            } else {
                withdrawCauseOpt = Optional.of(e);
            }
        }

        Update msg = builder.build();
        if (withdrawCauseOpt.isPresent()) {
            // Attempt to apply treat-as-withdraw
            msg = withdrawUpdate(msg, errorHandling, withdrawCauseOpt.get());
        }

        LOG.debug("BGP Update message was parsed {}.", msg);
        return msg;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private ParsedAttributes parseAttributes(final ByteBuf buffer, final int totalPathAttrLength,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
        try {
            return attrReg.parseAttributes(buffer.readSlice(totalPathAttrLength), constraint);
        } catch (final RuntimeException | BGPParsingException e) {
            // Catch everything else and turn it into a BGPDocumentedException
            throw new BGPDocumentedException("Could not parse BGP attributes.", BGPError.MALFORMED_ATTR_LIST, e);
        }
    }

    private static Ipv4Prefix readPrefix(final ByteBuf buf, final RevisedErrorHandling errorHandling,
            final String fieldName) throws BGPDocumentedException {
        final int prefixLength = buf.readUnsignedByte();
        if (errorHandling != RevisedErrorHandling.NONE) {
            // https://tools.ietf.org/html/rfc7606#section-5.3
            if (prefixLength > 32) {
                throw new BGPDocumentedException(fieldName + " length " + prefixLength + " exceeds 32 bytes",
                    BGPError.ATTR_LENGTH_ERROR);
            }
            if (prefixLength > buf.readableBytes() * 8) {
                throw new BGPDocumentedException(fieldName + " length " + prefixLength
                    + " exceeds unconsumed field space", BGPError.ATTR_LENGTH_ERROR);
            }
        }

        return Ipv4Util.prefixForByteBuf(buf, prefixLength);
    }

    /**
     * Check for presence of well known mandatory path attributes ORIGIN, AS_PATH and NEXT_HOP in Update message.
     *
     * @param message Update message
     * @param errorHandling Error handling type
     */
    private static void checkMandatoryAttributesPresence(final Update message,
            final RevisedErrorHandling errorHandling) throws BGPDocumentedException, BGPTreatAsWithdrawException {
        requireNonNull(message, "Update message cannot be null");

        final Attributes attrs = message.getAttributes();
        if (message.getNlri() != null && (attrs == null || attrs.getCNextHop() == null)) {
            throw reportMissingAttribute(errorHandling, "NEXT_HOP", NextHopAttributeParser.TYPE);
        }

        if (MessageUtil.isAnyNlriPresent(message)) {
            if (attrs == null || attrs.getOrigin() == null) {
                throw reportMissingAttribute(errorHandling, "ORIGIN", OriginAttributeParser.TYPE);
            }
            if (attrs.getAsPath() == null) {
                throw reportMissingAttribute(errorHandling, "AS_PATH", AsPathAttributeParser.TYPE);
            }
        }
    }

    private static BGPDocumentedException reportMissingAttribute(final RevisedErrorHandling errorHandling,
            final String attrName, final int attrType) throws BGPDocumentedException, BGPTreatAsWithdrawException {
        return errorHandling.reportError(BGPError.WELL_KNOWN_ATTR_MISSING, new byte[] { (byte) attrType },
            "Well known mandatory attribute missing: %s", attrName);
    }

    private Update withdrawUpdate(final Update parsed, final RevisedErrorHandling errorHandling,
            final BGPTreatAsWithdrawException withdrawCause) throws BGPDocumentedException {
        if (errorHandling == RevisedErrorHandling.NONE) {
            throw new BGPDocumentedException(withdrawCause);
        }

        // TODO: additional checks as per RFC7606 section 5.2

        LOG.debug("Converting BGP Update message {} to withdraw", parsed, withdrawCause);
        final UpdateBuilder builder = new UpdateBuilder();

        final List<Nlri> nlris = parsed.getNlri();
        final List<WithdrawnRoutes> withdrawn;
        if (nlris != null && !nlris.isEmpty()) {
            withdrawn = Streams.concat(parsed.nonnullWithdrawnRoutes().stream(),
                nlris.stream().map(nlri -> new WithdrawnRoutesBuilder(nlri).build()))
                    .collect(ImmutableList.toImmutableList());
        } else {
            withdrawn = parsed.getWithdrawnRoutes();
        }
        builder.setWithdrawnRoutes(withdrawn);

        final Attributes attributes = parsed.getAttributes();
        if (attributes != null) {
            builder.setAttributes(withdrawAttributes(attributes, withdrawCause));
        }

        return builder.build();
    }

    private Attributes withdrawAttributes(final Attributes parsed,
            final BGPTreatAsWithdrawException withdrawCause) throws BGPDocumentedException {
        final AttributesBuilder builder = new AttributesBuilder();
        final MpReachNlri mpReachNlri = getMpReach(parsed);
        if (mpReachNlri == null) {
            // No MP_REACH attribute, just reuse MP_UNREACH if it is present.
            final AttributesUnreach attrs2 = parsed.augmentation(AttributesUnreach.class);
            if (attrs2 != null) {
                builder.addAugmentation(attrs2);
            }
            return builder.build();
        }

        final MpUnreachNlri unreachNlri = getMpUnreach(parsed);
        if (unreachNlri != null) {
            final TablesKey reachKey = new TablesKey(mpReachNlri.getAfi(), mpReachNlri.getSafi());
            final TablesKey unreachKey = new TablesKey(unreachNlri.getAfi(), unreachNlri.getSafi());
            if (!reachKey.equals(unreachKey)) {
                LOG.warn("Unexpected mismatch between MP_REACH ({}) and MP_UNREACH ({})", reachKey, unreachKey,
                    withdrawCause);
                throw new BGPDocumentedException(withdrawCause);
            }
        }

        final MpUnreachNlri converted = nlriReg.convertMpReachToMpUnReach(mpReachNlri, unreachNlri)
                .orElseThrow(() -> {
                    LOG.warn("Could not convert attributes {} to withdraw attributes", parsed, withdrawCause);
                    return new BGPDocumentedException(withdrawCause);
                });

        builder.addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(converted).build());
        return builder.build();
    }

    private static MpReachNlri getMpReach(final Attributes attrs) {
        final AttributesReach reachAttr = attrs.augmentation(AttributesReach.class);
        return reachAttr == null ? null : reachAttr.getMpReachNlri();
    }

    private static MpUnreachNlri getMpUnreach(final Attributes attrs) {
        final AttributesUnreach unreachAttr = attrs.augmentation(AttributesUnreach.class);
        return unreachAttr == null ? null : unreachAttr.getMpUnreachNlri();
    }
}
