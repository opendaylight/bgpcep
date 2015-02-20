/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.LinkstatePathAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.LinkstatePathAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for Link State Path Attribute.
 *
 * @see <a href="http://tools.ietf.org/html/draft-gredler-idr-ls-distribution-04">BGP-LS draft</a>
 */
public class LinkstateAttributeParser implements AttributeParser, AttributeSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateAttributeParser.class);

    private static final int TYPE = 29;

    private static final int LEGACY_TYPE = 99;

    private final int type;

    public LinkstateAttributeParser(final boolean isIanaAssignedType) {
        this.type = (isIanaAssignedType) ? TYPE : LEGACY_TYPE;
    }

    public int getType() {
        return this.type;
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPParsingException {
        final NlriType nlriType = getNlriType(builder);
        if (nlriType == null) {
            LOG.warn("No Linkstate NLRI found, not parsing Linkstate attribute");
            return;
        }
        final PathAttributes1 a = new PathAttributes1Builder().setLinkstatePathAttribute(parseLinkState(nlriType, buffer)).build();
        builder.addAugmentation(PathAttributes1.class, a);
    }

    private NlriType getNlriType(final PathAttributesBuilder pab) {
        // we can extrapolate the variable, as there cannot be both MPReach & MPUnreach objects in PathAttributes
        NlriType type = null;
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1 mpr = pab.getAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1.class);
        if (mpr != null && mpr.getMpReachNlri() != null) {
            final DestinationType dt = mpr.getMpReachNlri().getAdvertizedRoutes().getDestinationType();
            if (dt instanceof DestinationLinkstateCase) {
                for (final CLinkstateDestination d : ((DestinationLinkstateCase) dt).getDestinationLinkstate().getCLinkstateDestination()) {
                    type = d.getNlriType();
                    break;
                }
            }
        }
        final PathAttributes2 mpu = pab.getAugmentation(PathAttributes2.class);
        if (mpu != null && mpu.getMpUnreachNlri() != null) {
            final DestinationType dt = mpu.getMpUnreachNlri().getWithdrawnRoutes().getDestinationType();
            if (dt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase) {
                for (final CLinkstateDestination d : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase) dt).getDestinationLinkstate().getCLinkstateDestination()) {
                    type = d.getNlriType();
                    break;
                }
            }
        }
        return type;
    }

    private static LinkstatePathAttribute parseLinkState(final NlriType nlri, final ByteBuf buffer) throws BGPParsingException {
        /*
         * e.g. IS-IS Area Identifier TLV can occur multiple times
         */
        final Multimap<Integer, ByteBuf> map = HashMultimap.create();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.readSlice(length);
            map.put(type, value);
        }
        final LinkstatePathAttributeBuilder builder = new LinkstatePathAttributeBuilder();

        switch (nlri) {
        case Ipv4Prefix:
        case Ipv6Prefix:
            builder.setLinkStateAttribute(PrefixAttributesParser.parsePrefixAttributes(map));
            return builder.build();
        case Link:
            builder.setLinkStateAttribute(LinkAttributesParser.parseLinkAttributes(map));
            return builder.build();
        case Node:
            builder.setLinkStateAttribute(NodeAttributesParser.parseNodeAttributes(map));
            return builder.build();
        default:
            throw new IllegalStateException("Unhandled NLRI type " + nlri);
        }
    }

    /**
     * Serialize linkstate attributes.
     *
     * @param attribute DataObject representing LinkstatePathAttribute
     * @param byteAggregator ByteBuf where all serialized data are aggregated
     */

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof PathAttributes, "Attribute parameter is not a PathAttribute object.");
        final PathAttributes1 pathAttributes1 = ((PathAttributes) attribute).getAugmentation(PathAttributes1.class);
        if (pathAttributes1 == null) {
            return;
        }
        final LinkStateAttribute linkState = pathAttributes1.getLinkstatePathAttribute().getLinkStateAttribute();
        final ByteBuf lsBuffer = Unpooled.buffer();
        if (linkState instanceof LinkAttributesCase) {
            LinkAttributesParser.serializeLinkAttributes((LinkAttributesCase) linkState, lsBuffer);
        } else if (linkState instanceof NodeAttributesCase) {
            NodeAttributesParser.serializeNodeAttributes((NodeAttributesCase) linkState, lsBuffer);
        } else if (linkState instanceof PrefixAttributesCase) {
            PrefixAttributesParser.serializePrefixAttributes((PrefixAttributesCase) linkState, lsBuffer);
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, getType(), lsBuffer, byteAggregator);
    }
}
