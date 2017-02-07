/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractETI;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractOrigRouteIp;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.IncMultiEthernetTagResCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.IncMultiEthernetTagResCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.inc.multi.ethernet.tag.res.IncMultiEthernetTagRes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.inc.multi.ethernet.tag.res.IncMultiEthernetTagResBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class IncMultEthTagRParser extends AbstractEvpnNlri {
    protected static final NodeIdentifier INC_MULT_ROUTE_NID = new NodeIdentifier(IncMultiEthernetTagRes.QNAME);
    private static final int CONTENT_LENGTH = 9;
    private static final int CONTENT_LENGTH2 = 21;

    @Override
    public EvpnChoice parseEvpn(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer.readableBytes() == CONTENT_LENGTH || buffer.readableBytes() == CONTENT_LENGTH2,
            "Wrong length of array of bytes. Passed: %s ;", buffer);

        final EthernetTagId eti = new EthernetTagIdBuilder().setVlanId(buffer.readUnsignedInt()).build();
        IpAddress ip = requireNonNull(EthSegRParser.parseOrigRouteIp(buffer));
        final IncMultiEthernetTagResBuilder builder = new IncMultiEthernetTagResBuilder().setEthernetTagId(eti).setOrigRouteIp(ip);
        return new IncMultiEthernetTagResCaseBuilder().setIncMultiEthernetTagRes(builder.build()).build();
    }

    @Override
    protected NlriType getType() {
        return NlriType.IncMultEthTag;
    }

    @Override
    public ByteBuf serializeBody(final EvpnChoice evpn) {
        Preconditions.checkArgument(evpn instanceof IncMultiEthernetTagResCase, "Unknown evpn instance. Passed %s. Needed IncMultiEthernetTagResCase.",
            evpn.getClass());
        return serializeBody(((IncMultiEthernetTagResCase) evpn).getIncMultiEthernetTagRes());
    }

    @Override
    public EvpnChoice serializeEvpnModel(final ContainerNode evpn) {
        return createRouteKey(evpn);
    }

    @Override
    public EvpnChoice createRouteKey(final ContainerNode evpn) {
        final IncMultiEthernetTagResBuilder builder = new IncMultiEthernetTagResBuilder();
        builder.setEthernetTagId(extractETI(evpn));
        builder.setOrigRouteIp(extractOrigRouteIp(evpn));
        return new IncMultiEthernetTagResCaseBuilder().setIncMultiEthernetTagRes(builder.build()).build();
    }

    private static ByteBuf serializeBody(final IncMultiEthernetTagRes evpn) {
        final ByteBuf body = Unpooled.buffer();
        ByteBufWriteUtil.writeUnsignedInt(evpn.getEthernetTagId().getVlanId(), body);
        final ByteBuf orig = EthSegRParser.serializeOrigRouteIp(evpn.getOrigRouteIp());
        Preconditions.checkArgument(orig.readableBytes() > 0);
        body.writeBytes(orig);
        return body;
    }
}
