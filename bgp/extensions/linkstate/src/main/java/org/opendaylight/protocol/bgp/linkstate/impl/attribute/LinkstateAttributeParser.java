/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.BiFunction;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for Link State Path Attribute.
 *
 * @see <a href="http://tools.ietf.org/html/draft-gredler-idr-ls-distribution-04">BGP-LS draft</a>
 */
public final class LinkstateAttributeParser extends AbstractAttributeParser implements AttributeSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateAttributeParser.class);

    private static final int TYPE = 29;

    private static final int LEGACY_TYPE = 99;

    private final int type;

    public LinkstateAttributeParser(final boolean isIanaAssignedType) {
        type = isIanaAssignedType ? TYPE : LEGACY_TYPE;
    }

    public static Multimap<Integer, ByteBuf> getAttributesMap(final ByteBuf buffer) {
        /*
         * e.g. IS-IS Area Identifier TLV can occur multiple times
         */
        final var map = HashMultimap.<Integer, ByteBuf>create();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.readSlice(length);
            map.put(type, value);
        }
        return map;
    }

    public int getType() {
        return type;
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint)
                    throws BGPParsingException {
        // FIXME: BGPCEP-359: we need an updated link-state spec for RFC7606
        final var lsDestination = getNlriType(builder);
        if (lsDestination == null) {
            LOG.warn("No Linkstate NLRI found, not parsing Linkstate attribute");
            return;
        }
        final var nlriType = lsDestination.getObjectType();
        final var protocolId = lsDestination.getProtocolId();
        builder.addAugmentation(new Attributes1Builder()
            .setLinkStateAttribute(parseLinkState(nlriType, protocolId, buffer))
            .build());
    }

    private static CLinkstateDestination getNlriType(final AttributesBuilder pab) {
        final var mpr = pab.augmentation(AttributesReach.class);
        if (mpr != null) {
            final var nlri = mpr.getMpReachNlri();
            if (nlri != null) {
                final var dt = nlri.getAdvertizedRoutes().getDestinationType();
                if (dt instanceof DestinationLinkstateCase dc) {
                    for (var d : dc.getDestinationLinkstate().nonnullCLinkstateDestination()) {
                        return d;
                    }
                }
            }
        }

        final var mpu = pab.augmentation(AttributesUnreach.class);
        if (mpu != null) {
            final var nlri = mpu.getMpUnreachNlri();
            if (nlri != null) {
                final var dt = mpu.getMpUnreachNlri().getWithdrawnRoutes().getDestinationType();
                if (dt
                    instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219
                        .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                        .DestinationLinkstateCase dc) {
                    for (var d : dc.getDestinationLinkstate().nonnullCLinkstateDestination()) {
                        return d;
                    }
                }
            }
        }
        return null;
    }

    private static LinkStateAttribute parseLinkState(final ObjectType nlri, final ProtocolId protocolId,
            final ByteBuf buffer) throws BGPParsingException {
        final BiFunction<Multimap<Integer, ByteBuf>, ProtocolId, LinkStateAttribute> parse = switch (nlri) {
            case PrefixCase prefix -> PrefixAttributesParser::parsePrefixAttributes;
            case LinkCase link -> LinkAttributesParser::parseLinkAttributes;
            case NodeCase node -> NodeAttributesParser::parseNodeAttributes;
            default -> throw new IllegalStateException("Unhandled NLRI type " + nlri);
        };
        return parse.apply(getAttributesMap(buffer), protocolId);
    }

    /**
     * Serialize linkstate attributes.
     *
     * @param attribute DataObject representing LinkstatePathAttribute
     * @param byteAggregator ByteBuf where all serialized data are aggregated
     */

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final var pathAttributes1 = attribute.augmentation(Attributes1.class);
        if (pathAttributes1 == null) {
            return;
        }
        final var linkState = pathAttributes1.getLinkStateAttribute();
        final ByteBuf lsBuffer = Unpooled.buffer();
        if (linkState instanceof LinkAttributesCase la) {
            LinkAttributesParser.serializeLinkAttributes(la, lsBuffer);
        } else if (linkState instanceof NodeAttributesCase na) {
            NodeAttributesParser.serializeNodeAttributes(na, lsBuffer);
        } else if (linkState instanceof PrefixAttributesCase pa) {
            PrefixAttributesParser.serializePrefixAttributes(pa, lsBuffer);
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, getType(), lsBuffer, byteAggregator);
    }
}
