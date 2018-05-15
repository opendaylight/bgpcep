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
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnParser;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.MulticastSourceRdGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.s.pmsi.a.d.grouping.SPmsiADBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;

/**
 * Abstract Mvpn Nlri.
 *
 * @author Claudio D. Gasparini
 */
abstract class AbstractMvpnNlri<T extends MvpnChoice> implements MvpnSerializer<T>, MvpnParser<T> {
    static final NodeIdentifier RD_NID = NodeIdentifier.create(QName.create(MvpnChoice.QNAME,
            "route-distinguisher").intern());
    static final NodeIdentifier ORI_NID = NodeIdentifier.create(QName.create(MvpnChoice.QNAME,
            "orig-route-ip").intern());
    static final NodeIdentifier SOURCE_AS_NID = NodeIdentifier.create(QName.create(MvpnChoice.QNAME,
            "source-as").intern());
    static final NodeIdentifier MULTICAST_SOURCE_NID = NodeIdentifier.create(QName.create(MvpnChoice.QNAME,
            "multicast-source").intern());
    static final NodeIdentifier MULTICAST_GROUP_NID = NodeIdentifier.create(QName.create(MvpnChoice.QNAME,
            "multicast-group").intern());

    @Override
    public final ByteBuf serializeMvpn(final T mvpn, final ByteBuf common) {
        final ByteBuf output = Unpooled.buffer();
        final ByteBuf body = serializeBody(mvpn);
        output.writeByte(getType());
        output.writeByte(body.readableBytes() + common.readableBytes());
        output.writeBytes(common);
        output.writeBytes(body);
        return output;
    }

    protected abstract ByteBuf serializeBody(T mvpn);


    final MulticastSourceRdGrouping parseRDMulticastSource(final ByteBuf buffer) {
        final SPmsiADBuilder builder = new SPmsiADBuilder();
        builder.setRouteDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(buffer));
        final IpAddress address = IpAddressUtil.addressForByteBuf(buffer);
        builder.setMulticastSource(address);
        return builder.build();
    }

    final void serializeRDMulticastSource(final MulticastSourceRdGrouping route, final ByteBuf output) {
        RouteDistinguisherUtil.serializeRouteDistinquisher(route.getRouteDistinguisher(), output);
        output.writeBytes(IpAddressUtil.bytesFor(route.getMulticastSource()));
    }
}
