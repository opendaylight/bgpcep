/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.parseRouteDistinguisher;
import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.serializeRouteDistinquisher;
import static org.opendaylight.protocol.util.MplsLabelUtil.byteBufForMplsLabel;
import static org.opendaylight.protocol.util.MplsLabelUtil.mplsLabelForByteBuf;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEsiTypeRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.IetfYangUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.MacIpAdvRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.MacIpAdvRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.MacIpAdvRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;

final class MACIpAdvRParser extends AbstractEvpnNlri {
    static final NodeIdentifier MAC_IP_ADV_ROUTE_CASE_NID = new NodeIdentifier(MacIpAdvRoute.QNAME);
    private static final int BITS_SIZE = 8;
    private static final NodeIdentifier IP_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "ip-address").intern());
    private static final NodeIdentifier MAC_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "mac-address").intern());
    private static final NodeIdentifier MPLS1_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "mpls-label1").intern());
    private static final NodeIdentifier MPLS2_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "mpls-label2").intern());
    private static final NodeIdentifier MAC_IP_ADV_ROUTE_NID = new NodeIdentifier(MacIpAdvRoute.QNAME);

    @Override
    public Evpn parseEvpn(final ByteBuf buffer) {
        final RouteDistinguisher rd = parseRouteDistinguisher(buffer);
        final Esi esi = SimpleEsiTypeRegistry.getInstance().parseEsi(buffer.readSlice(ESI_SIZE));
        final EthernetTagId eti = new EthernetTagIdBuilder().setVlanId(buffer.readUnsignedInt()).build();
        buffer.skipBytes(1);
        final MacAddress mac = IetfYangUtil.INSTANCE.macAddressFor(ByteArray.readBytes(buffer, MAC_ADDRESS_LENGTH));
        IpAddress ip = parseIpAddress(buffer);

        final MplsLabel label1 = mplsLabelForByteBuf(buffer);
        MplsLabel label2 = null;
        if (buffer.readableBytes() > 0) {
            label2 = mplsLabelForByteBuf(buffer);
        }
        return new MacIpAdvRouteCaseBuilder().setRouteDistinguisher(rd).setEsi(esi).setEthernetTagId(eti).setMacAddress(mac).setIpAddress(ip)
            .setMplsLabel1(label1).setMplsLabel2(label2).build();
    }

    @Override
    public void serializeEvpn(final Evpn evpn, final ByteBuf buffer) {
        Preconditions.checkArgument(evpn instanceof MacIpAdvRouteCase, "Unknown evpn instance. Passed %s. Needed MacIpAdvRouteCase.",
            evpn.getClass());
        serialize(NlriType.MacIpAdv.getIntValue(), serializeBody((MacIpAdvRouteCase) evpn), buffer);
    }

    @Override
    public Evpn serializeEvpnModel(final ChoiceNode evpnCase) {
        final ContainerNode evpn = (ContainerNode) evpnCase.getChild(MAC_IP_ADV_ROUTE_NID);
        final MacIpAdvRouteCaseBuilder builder = new MacIpAdvRouteCaseBuilder();
        builder.setRouteDistinguisher(extractRouteDistinguisher(evpn));
        builder.setEsi(serializeEsi((ChoiceNode) evpn.getChild(ESI_NID)));
        builder.setEthernetTagId(extractETI(evpn));
        builder.setMacAddress(extractMAC(evpn));
        builder.setIpAddress(extractIp(evpn));
        builder.setMplsLabel1(extractMplsLabel(evpn, MPLS1_NID));
        builder.setMplsLabel2(extractMplsLabel(evpn, MPLS2_NID));
        return builder.build();
    }

    private MacAddress extractMAC(final DataContainerNode<? extends PathArgument> evpn) {
        if (evpn.getChild(MAC_NID).isPresent()) {
            return new MacAddress((String) evpn.getChild(MAC_NID).get().getValue());
        }
        return null;
    }

    private ByteBuf serializeBody(final MacIpAdvRouteCase evpn) {
        final ByteBuf body = Unpooled.buffer();
        serializeRouteDistinquisher(evpn.getRouteDistinguisher(), body);
        SimpleEsiTypeRegistry.getInstance().serializeEsi(evpn.getEsi(), body);
        ByteBufWriteUtil.writeUnsignedInt(evpn.getEthernetTagId().getVlanId(), body);

        final MacAddress mac = evpn.getMacAddress();
        final int macLength = mac.getValue().getBytes().length * BITS_SIZE;//Is there some utility containing this info?
        body.writeByte(macLength);
        body.writeBytes(IetfYangUtil.INSTANCE.bytesFor(mac));
        body.writeBytes(byteBufForMplsLabel(evpn.getMplsLabel1()));
        return body;
    }

    private IpAddress extractIp(final DataContainerNode<? extends PathArgument> evpn) {
        if (evpn.getChild(IP_NID).isPresent()) {
            return IpAddressBuilder.getDefaultInstance((String) evpn.getChild(IP_NID).get().getValue());
        }
        return null;
    }
}
