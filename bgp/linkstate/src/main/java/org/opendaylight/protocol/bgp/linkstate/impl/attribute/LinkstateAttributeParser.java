/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.TeLspAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for Link State Path Attribute.
 *
 * @see <a href="http://tools.ietf.org/html/draft-gredler-idr-ls-distribution-04">BGP-LS draft</a>
 */
public final class LinkstateAttributeParser implements AttributeParser, AttributeSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateAttributeParser.class);

    private static final int TYPE = 29;

    private static final int LEGACY_TYPE = 99;

    private final int type;

    private final RSVPTeObjectRegistry rsvpTeObjectRegistry;

    public LinkstateAttributeParser(final boolean isIanaAssignedType, final RSVPTeObjectRegistry rsvpTeObjectRegistry) {
        this.type = isIanaAssignedType ? TYPE : LEGACY_TYPE;
        this.rsvpTeObjectRegistry = rsvpTeObjectRegistry;
    }

    private static Multimap<Integer, ByteBuf> getAttributesMap(final ByteBuf buffer) {
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
        return map;
    }

    public int getType() {
        return this.type;
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) throws BGPParsingException {
        final CLinkstateDestination lsDestination = getNlriType(builder);
        if (lsDestination == null) {
            LOG.warn("No Linkstate NLRI found, not parsing Linkstate attribute");
            return;
        }
        final ObjectType nlriType = lsDestination.getObjectType();
        final ProtocolId protocolId = lsDestination.getProtocolId();
        final Attributes1 a = new Attributes1Builder().setLinkStateAttribute(parseLinkState(nlriType, protocolId, buffer)).build();
        builder.addAugmentation(Attributes1.class, a);
    }

    private static CLinkstateDestination getNlriType(final AttributesBuilder pab) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1 mpr = pab.getAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1.class);
        if (mpr != null && mpr.getMpReachNlri() != null) {
            final DestinationType dt = mpr.getMpReachNlri().getAdvertizedRoutes().getDestinationType();
            if (dt instanceof DestinationLinkstateCase) {
                for (final CLinkstateDestination d : ((DestinationLinkstateCase) dt).getDestinationLinkstate().getCLinkstateDestination()) {
                    return d;
                }
            }
        }
        final Attributes2 mpu = pab.getAugmentation(Attributes2.class);
        if (mpu != null && mpu.getMpUnreachNlri() != null) {
            final DestinationType dt = mpu.getMpUnreachNlri().getWithdrawnRoutes().getDestinationType();
            if (dt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase) {
                for(final CLinkstateDestination d :((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.
                    routes.destination.type.DestinationLinkstateCase) dt).getDestinationLinkstate().getCLinkstateDestination()) {
                    return d;
                }
            }
        }
        return null;
    }

    private LinkStateAttribute parseLinkState(final ObjectType nlri, final ProtocolId protocolId, final ByteBuf buffer) throws BGPParsingException {

        if (nlri instanceof PrefixCase) {
            return PrefixAttributesParser.parsePrefixAttributes(getAttributesMap(buffer), protocolId);
        } else if (nlri instanceof LinkCase) {
            return LinkAttributesParser.parseLinkAttributes(getAttributesMap(buffer), protocolId);
        } else if (nlri instanceof NodeCase) {
            return NodeAttributesParser.parseNodeAttributes(getAttributesMap(buffer), protocolId);
        } else if (nlri instanceof TeLspCase) {
            return TeLspAttributesParser.parseTeLspAttributes(this.rsvpTeObjectRegistry, getAttributesMap(buffer)
                .entries().iterator().next().getValue());
        } else {
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
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes1 pathAttributes1 = ((Attributes) attribute).getAugmentation(Attributes1.class);
        if (pathAttributes1 == null) {
            return;
        }
        final LinkStateAttribute linkState = pathAttributes1.getLinkStateAttribute();
        final ByteBuf lsBuffer = Unpooled.buffer();
        if (linkState instanceof LinkAttributesCase) {
            LinkAttributesParser.serializeLinkAttributes((LinkAttributesCase) linkState, lsBuffer);
        } else if (linkState instanceof NodeAttributesCase) {
            NodeAttributesParser.serializeNodeAttributes((NodeAttributesCase) linkState, lsBuffer);
        } else if (linkState instanceof PrefixAttributesCase) {
            PrefixAttributesParser.serializePrefixAttributes((PrefixAttributesCase) linkState, lsBuffer);
        } else if (linkState instanceof TeLspAttributesCase) {
            TeLspAttributesParser.serializeLspAttributes(this.rsvpTeObjectRegistry, (TeLspAttributesCase) linkState, lsBuffer);
            byteAggregator.writeBytes(lsBuffer);
            return;
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, getType(), lsBuffer, byteAggregator);
    }
}
