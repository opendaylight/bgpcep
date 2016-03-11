/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi.pojo;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;

public class BGPPeerInstanceConfigurationTest {

    private static final InstanceConfigurationIdentifier INSTANCE_NAME = new InstanceConfigurationIdentifier("instanceName");
    private static final IpAddress HOST = new IpAddress(new Ipv4Address("127.0.0.1"));
    private static final PortNumber PORT = new PortNumber(1234);
    private static final short HOLD_TIMER = 180;
    private static final AsNumber AS_NUMBER = new AsNumber(72L);
    private static final String PASSWORD = new String("PASSWORD");

    private final BGPPeerInstanceConfiguration config = new BGPPeerInstanceConfiguration(INSTANCE_NAME, HOST, PORT, HOLD_TIMER, PeerRole.Ibgp,
            Boolean.FALSE, Collections.<BgpTableType>emptyList(), AS_NUMBER, PASSWORD);

    @Test
    public final void testGetHost() {
        assertEquals(HOST, config.getHost());
    }

    @Test
    public final void testGetPort() {
        assertEquals(PORT, config.getPort());
    }

    @Test
    public final void testGetHoldTimer() {
        assertEquals(HOLD_TIMER, config.getHoldTimer());
    }

    @Test
    public final void testGetPeerRole() {
        assertEquals(PeerRole.Ibgp, config.getPeerRole());
    }

    @Test
    public final void testIsActive() {
        assertEquals(Boolean.FALSE, config.isActive());
    }

    @Test
    public final void testGetAdvertizedTables() {
        assertEquals(Collections.EMPTY_LIST, config.getAdvertizedTables());
    }

    @Test
    public final void testGetAsNumber() {
        assertEquals(AS_NUMBER, config.getAsNumber());
    }

    @Test
    public final void testGetPassword() {
        assertEquals(PASSWORD, config.getPassword());
    }

    @Test
    public final void testGetInstanceName() {
        assertEquals(INSTANCE_NAME, config.getIdentifier());
    }

}
