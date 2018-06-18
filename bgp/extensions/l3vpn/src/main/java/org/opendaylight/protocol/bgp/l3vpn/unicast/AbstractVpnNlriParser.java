/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.unicast;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.labeled.unicast.LUNlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractVpnNlriParser implements NlriParser, NlriSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVpnNlriParser.class);

    protected abstract List<VpnDestination> getWithdrawnVpnDestination(DestinationType dstType);

    protected abstract List<VpnDestination> getAdvertizedVpnDestination(DestinationType dstType);

    protected abstract WithdrawnRoutes getWithdrawnRoutesByDestination(List<VpnDestination> dst);

    protected abstract AdvertizedRoutes getAdvertizedRoutesByDestination(List<VpnDestination> dst);

    private static void serializeNlri(final List<VpnDestination> dests,
            final boolean isWithdrawnRoute, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final VpnDestination dest : dests) {
            final List<LabelStack> labelStack = dest.getLabelStack();
            final IpPrefix prefix = dest.getPrefix();
            LOG.debug("Serializing Nlri: VpnDestination={}, IpPrefix={}", dest, prefix);
            AbstractVpnNlriParser.serializeLengtField(prefix, labelStack, nlriByteBuf);
            LUNlriParser.serializeLabelStackEntries(labelStack, isWithdrawnRoute, nlriByteBuf);
            RouteDistinguisherUtil.serializeRouteDistinquisher(dest.getRouteDistinguisher(), nlriByteBuf);
            Preconditions.checkArgument(prefix.getIpv6Prefix() != null || prefix.getIpv4Prefix() != null,
                    "Ipv6 or Ipv4 prefix is missing.");
            LUNlriParser.serializePrefixField(prefix, nlriByteBuf);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    /**
     * Serialize the length field Length field contains one Byte which represents the length of label stack and prefix
     * in bits.
     *
     * @param prefix      ipPrefix
     * @param labelStack  list of labelStack
     * @param nlriByteBuf ByteBuf
     */
    static void serializeLengtField(final IpPrefix prefix, final List<LabelStack> labelStack,
            final ByteBuf nlriByteBuf) {
        final int prefixLenght = LUNlriParser.getPrefixLength(prefix);
        int labelStackLenght = 0;
        if (labelStack != null) {
            labelStackLenght = LUNlriParser.LABEL_LENGTH * labelStack.size();
        }
        nlriByteBuf.writeByte((labelStackLenght + prefixLenght + RouteDistinguisherUtil.RD_LENGTH) * Byte.SIZE);
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final Attributes1 pathAttributes1 = pathAttributes.augmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.augmentation(Attributes2.class);
        List<VpnDestination> vpnDst = null;
        boolean isWithdrawnRoute = false;
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null) {
                vpnDst = getAdvertizedVpnDestination(routes.getDestinationType());
            }
        } else if (pathAttributes2 != null) {
            final WithdrawnRoutes routes = pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes();
            if (routes != null) {
                vpnDst = getWithdrawnVpnDestination(routes.getDestinationType());
                isWithdrawnRoute = true;
            }
        }
        if (vpnDst != null) {
            serializeNlri(vpnDst, isWithdrawnRoute, byteAggregator);
        }
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder,
        final PeerSpecificParserConstraint constraint) {
        if (!nlri.isReadable()) {
            return;
        }
        final List<VpnDestination> dst = VpnDestinationUtil.parseNlri(nlri, constraint,
                builder.getAfi(), builder.getSafi());
        builder.setWithdrawnRoutes(getWithdrawnRoutesByDestination(dst));
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder,
        final PeerSpecificParserConstraint constraint) {
        if (!nlri.isReadable()) {
            return;
        }
        final List<VpnDestination> dst = VpnDestinationUtil.parseNlri(nlri, constraint,
                builder.getAfi(), builder.getSafi());
        builder.setAdvertizedRoutes(getAdvertizedRoutesByDestination(dst));
    }
}
