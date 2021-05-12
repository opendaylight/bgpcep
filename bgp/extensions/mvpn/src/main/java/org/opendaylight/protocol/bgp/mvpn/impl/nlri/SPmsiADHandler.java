/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.bgp.concepts.IpAddressUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SPmsiADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SPmsiADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.s.pmsi.a.d.grouping.SPmsiAD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.s.pmsi.a.d.grouping.SPmsiADBuilder;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.3.
 *
 * @author Claudio D. Gasparini
 */
public final class SPmsiADHandler extends AbstractMvpnNlri<SPmsiADCase> {
    public SPmsiADHandler() {
        super(SPmsiADCase.class, NlriType.SPmsiAD);
    }

    @Override
    public SPmsiADCase parseMvpn(final ByteBuf buffer) {
        return new SPmsiADCaseBuilder()
            .setSPmsiAD(new SPmsiADBuilder(parseRDMulticastSource(buffer))
                .setMulticastGroup(MulticastGroupOpaqueUtil.multicastGroupForByteBuf(buffer))
                .setOrigRouteIp(IpAddressUtil.addressForByteBufWOLength(buffer))
                .build())
            .build();
    }

    @Override
    protected ByteBuf serializeBody(final SPmsiADCase mvpn) {
        final SPmsiAD route = mvpn.getSPmsiAD();
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        serializeRDMulticastSource(route, nlriByteBuf);
        MulticastGroupOpaqueUtil.bytesForMulticastGroup(route.getMulticastGroup(), nlriByteBuf);
        nlriByteBuf.writeBytes(IpAddressUtil.bytesWOLengthFor(route.getOrigRouteIp()));
        return nlriByteBuf;
    }
}
