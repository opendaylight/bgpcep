/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.MPLS_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractETI;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractMplsLabel;
import static org.opendaylight.protocol.util.MplsLabelUtil.byteBufForMplsLabel;
import static org.opendaylight.protocol.util.MplsLabelUtil.mplsLabelForByteBuf;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEsiTypeRegistry;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.a.d.route.EthernetADRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.a.d.route.EthernetADRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.EthernetADRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.EthernetADRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class EthADRParser extends AbstractEvpnNlri {
    protected static final NodeIdentifier ETH_AD_ROUTE_NID = new NodeIdentifier(EthernetADRoute.QNAME);
    private static final int CONTENT_LENGTH = 17;

    @Override
    public EvpnChoice parseEvpn(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer.readableBytes() == CONTENT_LENGTH, "Wrong length of array of bytes. Passed: %s ;", buffer);
        final Esi esi = SimpleEsiTypeRegistry.getInstance().parseEsi(buffer.readSlice(ESI_SIZE));
        final EthernetTagId eti = new EthernetTagIdBuilder().setVlanId(buffer.readUnsignedInt()).build();
        final MplsLabel label = mplsLabelForByteBuf(buffer);
        final EthernetADRouteBuilder builder = new EthernetADRouteBuilder().setEsi(esi).setEthernetTagId(eti).setMplsLabel(label);
        return new EthernetADRouteCaseBuilder().setEthernetADRoute(builder.build()).build();
    }

    @Override
    protected NlriType getType() {
        return NlriType.EthADDisc;
    }

    @Override
    public ByteBuf serializeBody(final EvpnChoice evpn) {
        Preconditions.checkArgument(evpn instanceof EthernetADRouteCase, "Unknown evpn instance. Passed %s. Needed EthernetADRouteCase.", evpn.getClass());
        return serializeBody(((EthernetADRouteCase) evpn).getEthernetADRoute());
    }

    @Override
    public EvpnChoice serializeEvpnModel(final ContainerNode evpn) {
        final EthernetADRouteBuilder builder = serializeKeyModel(evpn);
        builder.setMplsLabel(extractMplsLabel(evpn, MPLS_NID));
        return new EthernetADRouteCaseBuilder().setEthernetADRoute(builder.build()).build();
    }

    @Override
    public EvpnChoice createRouteKey(final ContainerNode evpn) {
        return new EthernetADRouteCaseBuilder().setEthernetADRoute(serializeKeyModel(evpn).build()).build();
    }

    private static EthernetADRouteBuilder serializeKeyModel(final ContainerNode evpn) {
        final EthernetADRouteBuilder builder = new EthernetADRouteBuilder();
        builder.setEsi(serializeEsi(evpn));
        builder.setEthernetTagId(extractETI(evpn));
        return builder;
    }

    private static ByteBuf serializeBody(final EthernetADRoute evpn) {
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        SimpleEsiTypeRegistry.getInstance().serializeEsi(evpn.getEsi(), body);
        ByteBufWriteUtil.writeUnsignedInt(evpn.getEthernetTagId().getVlanId(), body);

        final MplsLabel mpls = evpn.getMplsLabel();
        if (mpls != null) {
            body.writeBytes(byteBufForMplsLabel(evpn.getMplsLabel()));
        }
        return body;
    }
}