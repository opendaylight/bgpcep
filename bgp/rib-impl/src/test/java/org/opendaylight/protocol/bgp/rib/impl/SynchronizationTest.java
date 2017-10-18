/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class SynchronizationTest {

    private final TablesKey ipv4 = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private final TablesKey linkstate = new TablesKey(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);

    private BGPSynchronization bs;

    private SimpleSessionListener listener;

    private Update ipv4m;

    private Update ipv6m;

    private Update lsm;

    private Update eorm;

    @Before
    public void setUp() {
        this.listener = new SimpleSessionListener();
        this.ipv4m = new UpdateBuilder()
                .setNlri(Collections.singletonList(new NlriBuilder().setPrefix(new Ipv4Prefix("1.1.1.1/32")).build()))
                .build();

        MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(UnicastSubsequentAddressFamily.class);

        AttributesBuilder paBuilder = new AttributesBuilder().addAugmentation(Attributes1.class,
                new Attributes1Builder().setMpReachNlri(mpBuilder.build()).build());

        this.ipv6m = new UpdateBuilder().setAttributes(paBuilder.build()).build();

        mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(LinkstateAddressFamily.class);
        mpBuilder.setSafi(LinkstateSubsequentAddressFamily.class);

        paBuilder = new AttributesBuilder().addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(
                mpBuilder.build()).build());

        this.lsm = new UpdateBuilder().setAttributes(paBuilder.build()).build();

        this.eorm = new UpdateBuilder().build();

        this.bs = new BGPSynchronization(this.listener, Sets.newHashSet(this.ipv4, this.linkstate));
    }

    @Test
    public void testSynchronize() {
        // simulate sync
        this.bs.updReceived(this.ipv6m);
        this.bs.updReceived(this.ipv4m);
        this.bs.updReceived(this.lsm);
        this.bs.kaReceived(); // nothing yet
        assertFalse(this.bs.syncStorage.get(this.linkstate).getEor());
        assertFalse(this.bs.syncStorage.get(this.ipv4).getEor());
        this.bs.updReceived(this.ipv4m);
        this.bs.kaReceived(); // linkstate
        assertTrue(this.bs.syncStorage.get(this.linkstate).getEor());
        this.bs.kaReceived(); // ipv4 sync
        assertTrue(this.bs.syncStorage.get(this.ipv4).getEor());
    }

    @Test
    public void testSynchronizeWithEOR() {
        this.bs.updReceived(this.ipv4m);
        this.bs.updReceived(this.lsm);
        // Ipv4 Unicast synchronized by EOR message
        assertFalse(this.bs.syncStorage.get(this.ipv4).getEor());
        this.bs.updReceived(this.eorm);
        assertTrue(this.bs.syncStorage.get(this.ipv4).getEor());
        // Linkstate not synchronized yet
        assertFalse(this.bs.syncStorage.get(this.linkstate).getEor());
        this.bs.kaReceived();
        // no message sent by BGPSychchronization
        assertEquals(0, this.listener.getListMsg().size());
        this.bs.kaReceived();
        assertTrue(this.bs.syncStorage.get(this.linkstate).getEor());
    }
}
