/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.MPLS1_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.MPLS2_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractETI;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractIp;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractMAC;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractMplsLabel;
import static org.opendaylight.protocol.util.MplsLabelUtil.byteBufForMplsLabel;
import static org.opendaylight.protocol.util.MplsLabelUtil.mplsLabelForByteBuf;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEsiTypeRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.IetfYangUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.MacIpAdvRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.MacIpAdvRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.mac.ip.adv.route.MacIpAdvRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.mac.ip.adv.route.MacIpAdvRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class MACIpAdvRParser extends AbstractEvpnNlri {
    static final NodeIdentifier MAC_IP_ADV_ROUTE_NID = new NodeIdentifier(MacIpAdvRoute.QNAME);
    private static final int BITS_SIZE = 8;

    @Override
    public EvpnChoice parseEvpn(final ByteBuf buffer) {
        final Esi esi = SimpleEsiTypeRegistry.getInstance().parseEsi(buffer.readSlice(ESI_SIZE));
        final EthernetTagId eti = new EthernetTagIdBuilder().setVlanId(buffer.readUnsignedInt()).build();
        buffer.skipBytes(1);
        final MacAddress mac = IetfYangUtil.INSTANCE.macAddressFor(ByteArray.readBytes(buffer, MAC_ADDRESS_LENGTH));
        final IpAddress ip = parseIp(buffer);
        final MplsLabel label1 = mplsLabelForByteBuf(buffer);
        MplsLabel label2;
        if (buffer.readableBytes() > 0) {
            label2 = mplsLabelForByteBuf(buffer);
        } else {
            label2 = null;
        }
        final MacIpAdvRouteBuilder builder = new MacIpAdvRouteBuilder().setEsi(esi).setEthernetTagId(eti)
                .setMacAddress(mac).setIpAddress(ip)
            .setMplsLabel1(label1).setMplsLabel2(label2);
        return new MacIpAdvRouteCaseBuilder().setMacIpAdvRoute(builder.build()).build();
    }

    @Override
    protected NlriType getType() {
        return NlriType.MacIpAdv;
    }

    @Override
    public ByteBuf serializeBody(final EvpnChoice evpnChoice) {
        Preconditions.checkArgument(evpnChoice instanceof MacIpAdvRouteCase,
                "Unknown evpn instance. Passed %s. Needed MacIpAdvRouteCase.", evpnChoice.getClass());

        final ByteBuf body = Unpooled.buffer();
        final MacIpAdvRoute evpn = ((MacIpAdvRouteCase) evpnChoice).getMacIpAdvRoute();
        final Esi esi = evpn.getEsi();
        if (esi != null) {
            SimpleEsiTypeRegistry.getInstance().serializeEsi(evpn.getEsi(), body);
        }
        ByteBufWriteUtil.writeUnsignedInt(evpn.getEthernetTagId().getVlanId(), body);

        final MacAddress mac = evpn.getMacAddress();
        body.writeByte(MAC_ADDRESS_LENGTH * BITS_SIZE);
        body.writeBytes(IetfYangUtil.INSTANCE.bytesFor(mac));
        final ByteBuf ipAddress = serializeIp(evpn.getIpAddress());
        Preconditions.checkArgument(ipAddress.readableBytes() > 0);
        body.writeBytes(ipAddress);
        final MplsLabel mpls1 = evpn.getMplsLabel1();
        if (mpls1 != null) {
            body.writeBytes(byteBufForMplsLabel(mpls1));
        }
        final MplsLabel mpls2 = evpn.getMplsLabel2();
        if (mpls2 != null) {
            body.writeBytes(byteBufForMplsLabel(mpls2));
        }
        return body;
    }

    @Override
    public EvpnChoice serializeEvpnModel(final ContainerNode evpn) {
        final MacIpAdvRouteBuilder builder = serializeKeyModel(evpn);
        builder.setEsi(serializeEsi(evpn));
        builder.setMplsLabel1(extractMplsLabel(evpn, MPLS1_NID));
        builder.setMplsLabel2(extractMplsLabel(evpn, MPLS2_NID));
        return new MacIpAdvRouteCaseBuilder().setMacIpAdvRoute(builder.build()).build();
    }

    @Override
    public EvpnChoice createRouteKey(final ContainerNode evpn) {
        return new MacIpAdvRouteCaseBuilder().setMacIpAdvRoute(serializeKeyModel(evpn).build()).build();
    }

    private static MacIpAdvRouteBuilder serializeKeyModel(final ContainerNode evpn) {
        final MacIpAdvRouteBuilder builder = new MacIpAdvRouteBuilder();
        builder.setEthernetTagId(extractETI(evpn));
        builder.setMacAddress(extractMAC(evpn));
        builder.setIpAddress(extractIp(evpn));
        return builder;
    }

    private static ByteBuf serializeIp(final IpAddress ipAddress) {
        final ByteBuf body = Unpooled.buffer();
        if (ipAddress != null) {
            if (ipAddress.getIpv4Address() != null) {
                body.writeByte(Ipv4Util.IP4_BITS_LENGTH);
                body.writeBytes(Ipv4Util.bytesForAddress(ipAddress.getIpv4Address()));
            } else if (ipAddress.getIpv6Address() != null) {
                body.writeByte(Ipv6Util.IPV6_BITS_LENGTH);
                body.writeBytes(Ipv6Util.bytesForAddress(ipAddress.getIpv6Address()));
            } else {
                body.writeZero(ZERO_BYTE);
            }
        } else {
            body.writeZero(ZERO_BYTE);
        }
        return body;
    }

    private static IpAddress parseIp(final ByteBuf buffer) {
        final int ipLength = buffer.readUnsignedByte();
        if (ipLength == Ipv6Util.IPV6_BITS_LENGTH) {
            return new IpAddress(Ipv6Util.addressForByteBuf(buffer));
        } else if (ipLength == Ipv4Util.IP4_BITS_LENGTH) {
            return new IpAddress(Ipv4Util.addressForByteBuf(buffer));
        }
        return null;
    }
}
