/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.labeled.unicast.LUNlriParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.L3vpn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
public abstract class AbstractVpnNlriParser<VPN_DST extends L3vpn>
    implements NlriParser, NlriSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVpnNlriParser.class);

    protected abstract List<VPN_DST> getWithdrawnVpnDestination(DestinationType dstType);

    protected abstract List<VPN_DST> getAdvertizedVpnDestination(DestinationType dstType);

    protected abstract WithdrawnRoutes getWithdrawnRoutesByDestination(List<VPN_DST> dst);

    protected abstract AdvertizedRoutes getAdvertizedRoutesByDestination(List<VPN_DST> dst);

    public static <VPN_DST extends L3vpn> void serializeNlri(final List<VPN_DST> dests, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final VPN_DST dest : dests) {
            final List<LabelStack> labelStack = dest.getLabelStack();
            final IpPrefix prefix = dest.getPrefix();
            LOG.debug("Serializing Nlri: VpnDestination={}, IpPrefix={}", dest, prefix);
            // Serialize the length field
            // Length field contains one Byte which represents the length of label stack and prefix in bits
            nlriByteBuf.writeByte(((LUNlriParser.LABEL_LENGTH * labelStack.size()) + LUNlriParser.getPrefixLength(prefix) + RouteDistinguisherUtil.RD_LENGTH) * Byte.SIZE);
            LUNlriParser.serializeLabelStackEntries(labelStack, nlriByteBuf);
            RouteDistinguisherUtil.serializeRouteDistinquisher(dest.getRouteDistinguisher(), nlriByteBuf);
            Preconditions.checkArgument(prefix.getIpv6Prefix() != null || prefix.getIpv4Prefix() != null, "Ipv6 or Ipv4 prefix is missing.");
            LUNlriParser.serializePrefixField(prefix, nlriByteBuf);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    protected abstract List<VPN_DST> parseNlri(final ByteBuf nlri, final Class<? extends AddressFamily> afi);

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a Attributes object");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        List<VPN_DST> vpnDst = null;
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null) {
                vpnDst = getAdvertizedVpnDestination(routes.getDestinationType());
            }
        } else if (pathAttributes2 != null) {
            final WithdrawnRoutes routes = pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes();
            if (routes != null) {
                vpnDst = getWithdrawnVpnDestination(routes.getDestinationType());
            }
        }
        if (vpnDst != null)
            serializeNlri(vpnDst, byteAggregator);
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<VPN_DST> dst = parseNlri(nlri, builder.getAfi());
        builder.setWithdrawnRoutes(getWithdrawnRoutesByDestination(dst));
    }

    @Override
    public void parseNlri(final ByteBuf nlri, MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<VPN_DST> dst = parseNlri(nlri, builder.getAfi());
        builder.setAdvertizedRoutes(getAdvertizedRoutesByDestination(dst));
    }
}
