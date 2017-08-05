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
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.NextHopAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.WithdrawnRoutesBuilder;
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
        this.reg = Preconditions.checkNotNull(reg);
    }

    @Override
    public Update parseMessageBody(final ByteBuf buffer, final int messageLength) throws BGPDocumentedException {
        return parseMessageBody(buffer, messageLength, null);
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf bytes) {
        Preconditions.checkArgument(message instanceof Update, "Message needs to be of type Update");
        final Update update = (Update) message;

        final ByteBuf messageBody = Unpooled.buffer();
        final WithdrawnRoutes withdrawnRoutes = update.getWithdrawnRoutes();
        if (withdrawnRoutes != null) {
            final ByteBuf withdrawnRoutesBuf = Unpooled.buffer();
            for (final Ipv4Prefix prefix : withdrawnRoutes.getWithdrawnRoutes()) {
                ByteBufWriteUtil.writeMinimalPrefix(prefix, withdrawnRoutesBuf);
            }
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
        final Nlri nlri = update.getNlri();
        if (nlri != null && nlri.getNlri() !=null) {
            for (final Ipv4Prefix prefix : nlri.getNlri()) {
                ByteBufWriteUtil.writeMinimalPrefix(prefix, messageBody);
            }
        }
        MessageUtil.formatMessage(TYPE, messageBody, bytes);
    }

    /**
     * Parse Update message from buffer.
     * Calls {@link #checkMandatoryAttributesPresence(Update)} to check for presence of mandatory attributes.
     *
     * @param buffer Encoded BGP message in ByteBuf
     * @param messageLength Length of the BGP message
     * @param constraint Peer specific constraints
     * @return Parsed Update message body
     * @throws BGPDocumentedException
     */
    @Override
    public Update parseMessageBody(final ByteBuf buffer, final int messageLength, final PeerSpecificParserConstraint constraint)
            throws BGPDocumentedException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Buffer cannot be null or empty.");

        final UpdateBuilder builder = new UpdateBuilder();

        final int withdrawnRoutesLength = buffer.readUnsignedShort();
        if (withdrawnRoutesLength > 0) {
            // TODO handle NLRI with multiple paths - requires modified yang data model
            final List<Ipv4Prefix> withdrawnRoutes = Ipv4Util.prefixListForBytes(ByteArray.readBytes(buffer, withdrawnRoutesLength));
            builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setWithdrawnRoutes(withdrawnRoutes).build());
        }
        final int totalPathAttrLength = buffer.readUnsignedShort();

        if (withdrawnRoutesLength == 0 && totalPathAttrLength == 0) {
            return builder.build();
        }
        if (totalPathAttrLength > 0) {
            try {
                final Attributes attributes = this.reg.parseAttributes(buffer.readSlice(totalPathAttrLength), constraint);
                builder.setAttributes(attributes);
            } catch (final RuntimeException | BGPParsingException e) {
                // Catch everything else and turn it into a BGPDocumentedException
                throw new BGPDocumentedException("Could not parse BGP attributes.", BGPError.MALFORMED_ATTR_LIST, e);
            }
        }
        final List<Ipv4Prefix> nlri = Ipv4Util.prefixListForBytes(ByteArray.readAllBytes(buffer));
        if (!nlri.isEmpty()) {
            // TODO handle NLRI with multiple paths - requires modified yang data model
            builder.setNlri(new NlriBuilder().setNlri(nlri).build());
        }
        final Update msg = builder.build();
        checkMandatoryAttributesPresence(msg);
        LOG.debug("BGP Update message was parsed {}.", msg);
        return msg;
    }

    /**
     * Check for presence of well known mandatory path attributes
     * ORIGIN, AS_PATH and NEXT_HOP in Update message
     *
     * @param message Update message
     * @throws BGPDocumentedException
     */
    private static void checkMandatoryAttributesPresence(final Update message) throws BGPDocumentedException {
        Preconditions.checkNotNull(message, "Update message cannot be null");

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

            if (attrs == null || attrs.getAsPath() == null) {
                throw new BGPDocumentedException(BGPError.MANDATORY_ATTR_MISSING_MSG + "AS_PATH",
                        BGPError.WELL_KNOWN_ATTR_MISSING,
                        new byte[] { AsPathAttributeParser.TYPE });
            }
        }
    }
}
