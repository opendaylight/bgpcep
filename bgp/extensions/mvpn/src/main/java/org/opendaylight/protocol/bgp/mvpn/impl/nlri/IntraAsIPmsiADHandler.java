/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.bgp.concepts.IpAddressUtil;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.intra.as.i.pmsi.a.d.grouping.IntraAsIPmsiAD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.intra.as.i.pmsi.a.d.grouping.IntraAsIPmsiADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.IntraAsIPmsiADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.IntraAsIPmsiADCaseBuilder;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.1.
 *
 * @author Claudio D. Gasparini
 */
public final class IntraAsIPmsiADHandler extends AbstractMvpnNlri<IntraAsIPmsiADCase> {
    private static final int IPV4_CONTENT_LENGTH = 12;
    private static final int IPV6_CONTENT_LENGTH = 24;

    public IntraAsIPmsiADHandler() {
        super(IntraAsIPmsiADCase.class, NlriType.IntraAsIPmsiAD);
    }

    @Override
    public IntraAsIPmsiADCase parseMvpn(final ByteBuf buffer) {
        checkArgument(buffer.readableBytes() == IPV4_CONTENT_LENGTH || buffer.readableBytes() == IPV6_CONTENT_LENGTH,
            "Wrong length of array of bytes. Passed: %s ;", buffer);
        return new IntraAsIPmsiADCaseBuilder()
            .setIntraAsIPmsiAD(new IntraAsIPmsiADBuilder()
                .setRouteDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(buffer))
                .setOrigRouteIp(IpAddressUtil.addressForByteBufWOLength(buffer))
                .build())
            .build();
    }

    @Override
    protected ByteBuf serializeBody(final IntraAsIPmsiADCase mvpn) {
        final IntraAsIPmsiAD route = mvpn.getIntraAsIPmsiAD();
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        RouteDistinguisherUtil.serializeRouteDistinquisher(route.getRouteDistinguisher(), nlriByteBuf);
        serializeAddress(route.getOrigRouteIp(), nlriByteBuf);
        return nlriByteBuf;
    }
}
