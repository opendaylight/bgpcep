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

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEsiTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EsRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EsRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EsRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class EthSegRParser extends AbstractEvpnNlri {
    static final NodeIdentifier ES_ROUTE_CASE_NID = new NodeIdentifier(EsRouteCase.QNAME);
    private static final NodeIdentifier ES_ROUTE_NID = new NodeIdentifier(EsRoute.QNAME);
    private static final int CONTENT_LENGTH = 23;
    private static final int CONTENT_LENGTH2 = 35;

    @Override
    public Evpn parseEvpn(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer.readableBytes() == CONTENT_LENGTH || buffer.readableBytes() == CONTENT_LENGTH2,
            "Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");

        final RouteDistinguisher rd = parseRouteDistinguisher(buffer);
        final Esi esi = SimpleEsiTypeRegistry.getInstance().parseEsi(buffer.readSlice(ESI_SIZE));
        final IpAddress ip = Preconditions.checkNotNull(parseIpAddress(buffer));

        return new EsRouteCaseBuilder().setRouteDistinguisher(rd).setEsi(esi).setOrigRouteIp(ip).build();
    }

    @Override
    public void serializeEvpn(final Evpn evpnInput, final ByteBuf buffer) {
        Preconditions.checkArgument(evpnInput instanceof EsRouteCase, "Unknown evpn instance. Passed %s. Needed EsRouteCase.",
            evpnInput.getClass());
        serialize(NlriType.EthSeg.getIntValue(), serializeBody((EsRouteCase) evpnInput), buffer);
    }

    @Override
    public Evpn serializeEvpnModel(final ChoiceNode evpnCase) {
        final ContainerNode evpn = (ContainerNode) evpnCase.getChild(ES_ROUTE_NID);
        final EsRouteCaseBuilder builder = new EsRouteCaseBuilder();
        builder.setRouteDistinguisher(extractRouteDistinguisher(evpn));
        builder.setEsi(serializeEsi((ChoiceNode) evpn.getChild(ESI_NID)));
        builder.setOrigRouteIp(extractOrigRouteIp(evpn));
        return builder.build();
    }

    private ByteBuf serializeBody(final EsRouteCase evpn) {
        final ByteBuf body = Unpooled.buffer();
        serializeRouteDistinquisher(evpn.getRouteDistinguisher(), body);
        SimpleEsiTypeRegistry.getInstance().serializeEsi(evpn.getEsi(), body);
        final ByteBuf orig = serializeIpAddress(evpn.getOrigRouteIp());
        Preconditions.checkArgument(orig.readableBytes() > 0);
        return body;
    }
}
