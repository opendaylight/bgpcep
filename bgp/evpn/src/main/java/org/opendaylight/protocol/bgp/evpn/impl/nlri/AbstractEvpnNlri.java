/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIModelParser;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnParser;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnSerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;

abstract class AbstractEvpnNlri implements EvpnParser, EvpnSerializer {
    protected static final NodeIdentifier RD_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "route-distinguisher").intern());
    static final int MAC_ADDRESS_LENGTH = 6;
    static final int ESI_SIZE = 10;
    static final NodeIdentifier ESI_NID = NodeIdentifier.create(Esi.QNAME);
    private static final int IPV6_LENGTH = Ipv6Util.IPV6_LENGTH * 8;
    private static final int ZERO_BYTE = 1;
    protected static final NodeIdentifier ETI_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "ethernet-tag-id").intern());
    private static final NodeIdentifier ORI_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "orig-route-ip").intern());

    protected final void serialize(final int nlriType, final ByteBuf body, final ByteBuf buffer) {
        buffer.writeByte(nlriType);
        buffer.writeByte(body.readableBytes());
        buffer.writeBytes(body);
    }

    protected final IpAddress parseIpAddress(final ByteBuf buffer) {
        final int ipLength = buffer.readByte();
        if (ipLength == IPV6_LENGTH) {
            return new IpAddress(Ipv6Util.addressForByteBuf(buffer));
        } else if (ipLength == Ipv4Util.IP4_LENGTH) {
            return new IpAddress(Ipv4Util.addressForByteBuf(buffer));
        }
        return null;
    }

    protected final ByteBuf serializeIpAddress(final IpAddress origRouteIp) {
        final ByteBuf body = Unpooled.buffer();
        if (origRouteIp.getIpv4Address() != null) {
            body.writeByte(Ipv4Util.IP4_LENGTH);
            body.writeBytes(Ipv4Util.bytesForAddress(origRouteIp.getIpv4Address()));
        } else if (origRouteIp.getIpv6Address() != null) {
            body.writeByte(Ipv6Util.IPV6_LENGTH);
            body.writeBytes(Ipv6Util.bytesForAddress(origRouteIp.getIpv6Address()));
        } else {
            body.writeZero(ZERO_BYTE);
        }
        return body;
    }


    protected static RouteDistinguisher extractRouteDistinguisher(final DataContainerNode<? extends PathArgument> evpn) {
        if (evpn.getChild(RD_NID).isPresent()) {
            return (RouteDistinguisher) evpn.getChild(RD_NID).get().getValue();
        }
        return null;
    }

    protected final MplsLabel extractMplsLabel(final DataContainerNode<? extends PathArgument> evpn, final NodeIdentifier mplsNid) {
        if (evpn.getChild(mplsNid).isPresent()) {
            return (MplsLabel) evpn.getChild(mplsNid).get().getValue();
        }
        return null;
    }

    protected final IpAddress extractOrigRouteIp(final DataContainerNode<? extends PathArgument> evpn) {
        if (evpn.getChild(ORI_NID).isPresent()) {
            return IpAddressBuilder.getDefaultInstance((String) evpn.getChild(ORI_NID).get().getValue());
        }
        return null;
    }

    protected final Esi serializeEsi(final ChoiceNode esi) {
        return ESIModelParser.parseModel(esi);
    }


    protected final EthernetTagId extractETI(final ContainerNode evpn) {
        if (evpn.getChild(ETI_NID).isPresent()) {
            return (EthernetTagId) evpn.getChild(ETI_NID).get().getValue();
        }
        return null;
    }
}
