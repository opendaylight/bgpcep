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
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.inter.as.i.pmsi.a.d.grouping.InterAsIPmsiAD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.inter.as.i.pmsi.a.d.grouping.InterAsIPmsiADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.InterAsIPmsiADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.InterAsIPmsiADCaseBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.2 .
 *
 * @author Claudio D. Gasparini
 */
public final class InterASIPmsiADHandler extends AbstractMvpnNlri<InterAsIPmsiADCase> {
    private static final int CONTENT_LENGTH = 12;

    public InterASIPmsiADHandler() {
        super(InterAsIPmsiADCase.class, NlriType.InterAsIPmsiAD);
    }

    @Override
    public InterAsIPmsiADCase parseMvpn(final ByteBuf buffer) {
        checkArgument(buffer.readableBytes() == CONTENT_LENGTH, "Wrong length of array of bytes. Passed: %s ;", buffer);
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
}
