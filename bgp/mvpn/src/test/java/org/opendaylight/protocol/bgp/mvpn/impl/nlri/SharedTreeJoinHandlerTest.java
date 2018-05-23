/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.c.multicast.grouping.CMulticastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.multicast.group.CGAddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.multicast.group.c.g.address._case.CGAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.SharedTreeJoinCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.SharedTreeJoinCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.shared.tree.join._case.SharedTreeJoinBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisher;

public final class SharedTreeJoinHandlerTest {
    private static final byte[] SHARED_TREE = new byte[]{
        0, 1, 1, 2, 3, 4, 1, 2,
        0, 0, 0, 10,
        32, 1, 0, 0, 1,
        32, 2, 0, 0, 2,
    };
    private static final byte[] SHARED_TREE_LENGTH = new byte[]{
        6, 22,
        0, 1, 1, 2, 3, 4, 1, 2,
        0, 0, 0, 10,
        32, 1, 0, 0, 1,
        32, 2, 0, 0, 2,
    };

    private final SharedTreeJoinCase expected = new SharedTreeJoinCaseBuilder()
            .setSharedTreeJoin(new SharedTreeJoinBuilder()
                    .setCMulticast(new CMulticastBuilder()
                            .setRouteDistinguisher(new RouteDistinguisher(new RdIpv4("1.2.3.4:258")))
                            .setSourceAs(new AsNumber(10L))
                            .setMulticastSource(new IpAddress(new Ipv4Address("1.0.0.1")))
                            .setMulticastGroup(new CGAddressCaseBuilder().setCGAddress(
                                    new CGAddressBuilder()
                                            .setCGAddress(new IpAddress(new Ipv4Address("2.0.0.2")))
                                            .build()).build()).build()).build()).build();
    private SharedTreeJoinHandler handler = new SharedTreeJoinHandler();

    @Test
    public void testParser() {
        assertEquals(this.expected, this.handler.parseMvpn(Unpooled.copiedBuffer(SHARED_TREE)));
    }

    @Test
    public void testSerializer() {
        final ByteBuf buffer = Unpooled.buffer(SHARED_TREE_LENGTH.length);
        assertArrayEquals(SHARED_TREE_LENGTH, ByteArray.getAllBytes(this.handler.serializeMvpn(this.expected, buffer)));
    }

    @Test
    public void testGetType() {
        assertEquals(NlriType.SharedTreeJoin.getIntValue(), this.handler.getType());
    }

    @Test
    public void testGetClazz() {
        assertEquals(SharedTreeJoinCase.class, this.handler.getClazz());
    }
}