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
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EthernetADRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EthernetADRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EthernetADRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class EthADRParser extends AbstractEvpnNlri {
    static final NodeIdentifier ETH_AD_ROUTE_CASE_NID = new NodeIdentifier(EthernetADRouteCase.QNAME);
    static final int CONTENT_LENGTH = 25;
    protected static final NodeIdentifier ETH_AD_ROUTE_NID = new NodeIdentifier(EthernetADRoute.QNAME);
    protected static final NodeIdentifier MPLS_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "mpls-label").intern());

    @Override
    public Evpn parseEvpn(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer.readableBytes() == CONTENT_LENGTH, "Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        final RouteDistinguisher rd = parseRouteDistinguisher(buffer);
        final Esi esi = SimpleEsiTypeRegistry.getInstance().parseEsi(buffer.readSlice(ESI_SIZE));
        final EthernetTagId eti = new EthernetTagIdBuilder().setVlanId(buffer.readUnsignedInt()).build();
        final MplsLabel label = mplsLabelForByteBuf(buffer);
        return new EthernetADRouteCaseBuilder().setRouteDistinguisher(rd).setEsi(esi).setEthernetTagId(eti).setMplsLabel(label).build();
    }

    @Override
    public void serializeEvpn(final Evpn evpn, final ByteBuf buffer) {
        Preconditions.checkArgument(evpn instanceof EthernetADRouteCase, "Unknown evpn instance. Passed %s. Needed EthernetADRouteCase.", evpn.getClass());
        serialize(NlriType.EthADDisc.getIntValue(), serializeBody((EthernetADRouteCase) evpn), buffer);
    }

    @Override
    public Evpn serializeEvpnModel(final ChoiceNode evpnCase) {
        final ContainerNode evpn = (ContainerNode) evpnCase.getChild(ETH_AD_ROUTE_NID).get();
        final EthernetADRouteCaseBuilder builder = new EthernetADRouteCaseBuilder();
        builder.setRouteDistinguisher(extractRouteDistinguisher(evpn));
        final ChoiceNode esiCase = (ChoiceNode) evpn.getChild(ESI_NID).get().getValue();
        builder.setEsi(serializeEsi(esiCase));
        builder.setEthernetTagId(extractETI(evpn));
        builder.setMplsLabel(extractMplsLabel(evpn, MPLS_NID));
        return builder.build();
    }

    private ByteBuf serializeBody(final EthernetADRouteCase evpn) {
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        serializeRouteDistinquisher(evpn.getRouteDistinguisher(), body);
        SimpleEsiTypeRegistry.getInstance().serializeEsi(evpn.getEsi(), body);
        ByteBufWriteUtil.writeUnsignedInt(evpn.getEthernetTagId().getVlanId(), body);
        body.writeBytes(byteBufForMplsLabel(evpn.getMplsLabel()));
        return body;
    }
}