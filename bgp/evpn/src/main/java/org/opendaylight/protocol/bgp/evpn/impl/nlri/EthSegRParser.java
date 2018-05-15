/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractOrigRouteIp;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.bgp.concepts.IpAddressUtil;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEsiTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.es.route.EsRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.es.route.EsRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.evpn.choice.EsRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.evpn.choice.EsRouteCaseBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class EthSegRParser extends AbstractEvpnNlri {
    static final NodeIdentifier ES_ROUTE_NID = new NodeIdentifier(EsRoute.QNAME);
    private static final int CONTENT_LENGTH = 15;
    private static final int CONTENT_LENGTH2 = 27;

    @Override
    public EvpnChoice parseEvpn(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer.readableBytes() == CONTENT_LENGTH
                        || buffer.readableBytes() == CONTENT_LENGTH2,
            "Wrong length of array of bytes. Passed: %s ;", buffer);

        final Esi esi = SimpleEsiTypeRegistry.getInstance().parseEsi(buffer.readSlice(ESI_SIZE));
        final IpAddress ip = IpAddressUtil.addressForByteBuf(buffer);

        final EsRouteBuilder builder = new EsRouteBuilder().setEsi(esi).setOrigRouteIp(ip);
        return new EsRouteCaseBuilder().setEsRoute(builder.build()).build();
    }

    @Override
    protected NlriType getType() {
        return NlriType.EthSeg;
    }

    @Override
    public ByteBuf serializeBody(final EvpnChoice evpnInput) {
        Preconditions.checkArgument(evpnInput instanceof EsRouteCase,
                "Unknown evpn instance. Passed %s. Needed EsRouteCase.", evpnInput.getClass());
        final EsRoute evpn = ((EsRouteCase) evpnInput).getEsRoute();
        final ByteBuf body = Unpooled.buffer();
        SimpleEsiTypeRegistry.getInstance().serializeEsi(evpn.getEsi(), body);
        final ByteBuf orig = IpAddressUtil.bytesFor(evpn.getOrigRouteIp());
        Preconditions.checkArgument(orig.readableBytes() > 0);
        body.writeBytes(orig);
        return body;
    }

    @Override
    public EvpnChoice serializeEvpnModel(final ContainerNode evpnCase) {
        return createRouteKey(evpnCase);
    }

    @Override
    public EvpnChoice createRouteKey(final ContainerNode evpn) {
        final EsRouteBuilder builder = new EsRouteBuilder();
        builder.setEsi(serializeEsi(evpn));
        builder.setOrigRouteIp(extractOrigRouteIp(evpn));
        return new EsRouteCaseBuilder().setEsRoute(builder.build()).build();
    }
}
