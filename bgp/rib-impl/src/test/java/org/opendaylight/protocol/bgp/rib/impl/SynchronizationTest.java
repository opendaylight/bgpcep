/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class SynchronizationTest {

    private BGPSynchronization bs;

    private SimpleSessionListener listener;

    private Update ipv4m;

    private Update ipv6m;

    private Update lsm;

    @Before
    public void setUp() {
        this.listener = new SimpleSessionListener();
        this.ipv4m = new UpdateBuilder().setNlri(new NlriBuilder().setNlri(Lists.newArrayList(new Ipv4Prefix("1.1.1.1/32"))).build()).build();

        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(UnicastSubsequentAddressFamily.class);

        PathAttributesBuilder paBuilder = new PathAttributesBuilder().addAugmentation(PathAttributes1.class,
                new PathAttributes1Builder().setMpReachNlri(mpBuilder.build()).build());

        this.ipv6m = new UpdateBuilder().setPathAttributes(paBuilder.build()).build();

        final MpUnreachNlriBuilder mpUBuilder = new MpUnreachNlriBuilder();
        mpUBuilder.setAfi(LinkstateAddressFamily.class);
        mpUBuilder.setSafi(LinkstateSubsequentAddressFamily.class);

        paBuilder = new PathAttributesBuilder().addAugmentation(PathAttributes2.class, new PathAttributes2Builder().setMpUnreachNlri(
                mpUBuilder.build()).build());

        this.lsm = new UpdateBuilder().setPathAttributes(paBuilder.build()).build();

        final Set<TablesKey> types = Sets.newHashSet();
        types.add(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        types.add(new TablesKey(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

        this.bs = new BGPSynchronization(new BGPSession() {

            @Override
            public void close() {
            }

            @Override
            public Set<BgpTableType> getAdvertisedTableTypes() {
                final Set<BgpTableType> types = Sets.newHashSet();
                types.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
                types.add(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));
                return types;
            }

            @Override
            public Ipv4Address getBgpId() {
                return new Ipv4Address("127.0.0.1");
            }

            @Override
            public AsNumber getAsNumber() {
                return new AsNumber(30L);
            }
        }, this.listener, types);
    }

    @Test
    public void testSynchronize() {
        // simulate sync
        this.bs.updReceived(this.ipv6m);
        this.bs.updReceived(this.ipv4m);
        this.bs.updReceived(this.lsm);
        this.bs.kaReceived(); // nothing yet
        this.bs.updReceived(this.ipv4m);
        this.bs.kaReceived(); // linkstate
        assertEquals(1, this.listener.getListMsg().size());
        assertEquals(LinkstateAddressFamily.class, ((Update) this.listener.getListMsg().get(0)).getPathAttributes().getAugmentation(
                PathAttributes1.class).getMpReachNlri().getAfi());
        this.bs.kaReceived(); // ipv4 sync
        assertEquals(2, this.listener.getListMsg().size());
    }
}
