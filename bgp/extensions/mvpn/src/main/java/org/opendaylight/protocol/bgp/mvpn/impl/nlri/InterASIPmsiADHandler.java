/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.inter.as.i.pmsi.a.d.grouping.InterAsIPmsiAD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.inter.as.i.pmsi.a.d.grouping.InterAsIPmsiADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.InterAsIPmsiADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.InterAsIPmsiADCaseBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.2 .
 *
 * @author Claudio D. Gasparini
 */
public final class InterASIPmsiADHandler extends AbstractMvpnNlri<InterAsIPmsiADCase> {
    private static final int CONTENT_LENGTH = 12;

    @Override
    public int getType() {
        return NlriType.InterAsIPmsiAD.getIntValue();
    }

    @Override
    public InterAsIPmsiADCase parseMvpn(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer.readableBytes() == CONTENT_LENGTH,
                "Wrong length of array of bytes. Passed: %s ;", buffer);
        return new InterAsIPmsiADCaseBuilder()
                .setInterAsIPmsiAD(new InterAsIPmsiADBuilder()
                        .setRouteDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(buffer))
                        .setSourceAs(new AsNumber(ByteBufUtils.readUint32(buffer)))
                        .build())
                .build();
    }

    @Override
    protected ByteBuf serializeBody(final InterAsIPmsiADCase mvpn) {
        final InterAsIPmsiAD route = mvpn.getInterAsIPmsiAD();
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        RouteDistinguisherUtil.serializeRouteDistinquisher(route.getRouteDistinguisher(), nlriByteBuf);
        nlriByteBuf.writeInt(route.getSourceAs().getValue().intValue());
        return nlriByteBuf;
    }

    @Override
    public Class<? extends MvpnChoice> getClazz() {
        return InterAsIPmsiADCase.class;
    }
}
