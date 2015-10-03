/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi.pojo;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;

public class BGPRibInstanceConfigurationTest {

    private static final InstanceConfigurationIdentifier INSTANCE_NAME = new InstanceConfigurationIdentifier("instanceName");
    private static final Ipv4Address BGP_ID = new Ipv4Address("127.0.0.1");
    private static final Ipv4Address CLUSTER_ID = new Ipv4Address("127.0.0.2");
    private static final AsNumber AS_NUMBER = new AsNumber(72L);

    private final BGPRibInstanceConfiguration config = new BGPRibInstanceConfiguration(INSTANCE_NAME, AS_NUMBER, BGP_ID, CLUSTER_ID,
            Collections.<BgpTableType>emptyList());

    @Test
    public final void testGetLocalAs() {
        assertEquals(AS_NUMBER, config.getLocalAs());
    }

    @Test
    public final void testGetBgpRibId() {
        assertEquals(BGP_ID, config.getBgpRibId());
    }

    @Test
    public final void testGetClusterId() {
        assertEquals(Optional.of(CLUSTER_ID), config.getClusterId());
    }

    @Test
    public final void testGetTableTypes() {
        assertEquals(Collections.EMPTY_LIST, config.getTableTypes());
    }

    @Test
    public final void testGetInstanceName() {
        assertEquals(INSTANCE_NAME, config.getIdentifier());
    }

}
