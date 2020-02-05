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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SourceActiveADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SourceActiveADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.source.active.a.d.grouping.SourceActiveAD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.source.active.a.d.grouping.SourceActiveADBuilder;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.5.
 *
 * @author Claudio D. Gasparini
 */
public final class SourceActiveADHandler extends AbstractMvpnNlri<SourceActiveADCase> {
    @Override
    public int getType() {
        return NlriType.SourceActiveAD.getIntValue();
    }

    @Override
    public SourceActiveADCase parseMvpn(final ByteBuf buffer) {
        final SourceActiveADBuilder builder = new SourceActiveADBuilder(parseRDMulticastSource(buffer));
        builder.setMulticastGroup(IpAddressUtil.addressForByteBuf(buffer));
        return new SourceActiveADCaseBuilder().setSourceActiveAD(builder.build()).build();
    }


    @Override
    protected ByteBuf serializeBody(final SourceActiveADCase mvpn) {
        final SourceActiveAD route = mvpn.getSourceActiveAD();
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        serializeRDMulticastSource(route, nlriByteBuf);
        nlriByteBuf.writeBytes(IpAddressUtil.bytesFor(route.getMulticastGroup()));
        return nlriByteBuf;
    }

    @Override
    public Class<? extends MvpnChoice> getClazz() {
        return SourceActiveADCase.class;
    }
}
