/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractETI;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractOrigRouteIp;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.bgp.concepts.IpAddressUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.evpn.choice.IncMultiEthernetTagResCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.evpn.choice.IncMultiEthernetTagResCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.inc.multi.ethernet.tag.res.IncMultiEthernetTagRes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.inc.multi.ethernet.tag.res.IncMultiEthernetTagResBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class IncMultEthTagRParser extends AbstractEvpnNlri {
    static final NodeIdentifier INC_MULT_ROUTE_NID = NodeIdentifier.create(IncMultiEthernetTagRes.QNAME);
    private static final int CONTENT_LENGTH = 9;
    private static final int CONTENT_LENGTH2 = 21;

    private static EvpnChoice serializeModel(final ContainerNode evpn) {
        final IncMultiEthernetTagResBuilder builder = new IncMultiEthernetTagResBuilder();
        builder.setEthernetTagId(extractETI(evpn));
        builder.setOrigRouteIp(extractOrigRouteIp(evpn));
        return new IncMultiEthernetTagResCaseBuilder().setIncMultiEthernetTagRes(builder.build()).build();
    }

    @Override
    public EvpnChoice parseEvpn(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer.readableBytes() == CONTENT_LENGTH
                        || buffer.readableBytes() == CONTENT_LENGTH2,
                "Wrong length of array of bytes. Passed: %s ;", buffer);

        final EthernetTagId eti = new EthernetTagIdBuilder().setVlanId(ByteBufUtils.readUint32(buffer)).build();
        final IpAddressNoZone ip = IpAddressUtil.addressForByteBuf(buffer);
        final IncMultiEthernetTagResBuilder builder = new IncMultiEthernetTagResBuilder()
                .setEthernetTagId(eti).setOrigRouteIp(ip);
        return new IncMultiEthernetTagResCaseBuilder().setIncMultiEthernetTagRes(builder.build()).build();
    }

    @Override
    protected NlriType getType() {
        return NlriType.IncMultEthTag;
    }

    @Override
    public ByteBuf serializeBody(final EvpnChoice evpnChoice) {
        Preconditions.checkArgument(evpnChoice instanceof IncMultiEthernetTagResCase,
                "Unknown evpn instance. Passed %s. Needed IncMultiEthernetTagResCase.",
                evpnChoice.getClass());

        final IncMultiEthernetTagRes evpn = ((IncMultiEthernetTagResCase) evpnChoice).getIncMultiEthernetTagRes();
        final ByteBuf body = Unpooled.buffer();
        ByteBufUtils.writeOrZero(body, evpn.getEthernetTagId().getVlanId());
        IpAddressUtil.writeBytesFor(evpn.getOrigRouteIp(), body);
        return body;
    }

    @Override
    public EvpnChoice serializeEvpnModel(final ContainerNode evpn) {
        return serializeModel(evpn);
    }

    @Override
    public EvpnChoice createRouteKey(final ContainerNode evpn) {
        return serializeModel(evpn);
    }
}
