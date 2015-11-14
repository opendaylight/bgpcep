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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class ApiTest {

    @Test
    public void testBGPSessionProposalImpl() {
        final Class<? extends AddressFamily> key = LinkstateAddressFamily.class;
        final Class<? extends SubsequentAddressFamily> value = LinkstateSubsequentAddressFamily.class;
        final Map<Class<? extends AddressFamily>, Class<? extends SubsequentAddressFamily>> map = new HashMap<>();
        map.put(key, value);

        final BGPSessionProposalImpl proposal = new BGPSessionProposalImpl((short) 5, new AsNumber(58L), null, map, null);
        final BGPSessionPreferences sp = proposal.getProposal();
        assertNull(sp.getBgpId());
        assertEquals(proposal.getHoldTimer(), sp.getHoldTime());
        assertEquals(proposal.getAs(), sp.getMyAs());
        assertNull(proposal.getBgpId());
        assertNotNull(sp.getParams());

        final List<OptionalCapabilities> linkstate = new ArrayList<>();
        final List<OptionalCapabilities> ipv4 = new ArrayList<>();

        linkstate.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(
            CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                .setAfi(key).setSafi(value).build()).build()).build()).build());

        ipv4.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(
            CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                .setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build()).build()).build()).build());

        setOptionalCapabilities(linkstate);
        setOptionalCapabilities(ipv4);

        final BgpParameters rightInput = new BgpParametersBuilder().setOptionalCapabilities(linkstate).build();
        final BgpParameters wrongInput = new BgpParametersBuilder().setOptionalCapabilities(ipv4).build();
        assertNotNull(rightInput);
        assertTrue(sp.getParams().contains(rightInput));
        assertFalse(sp.getParams().contains(wrongInput));
    }

    private void setOptionalCapabilities(final List<OptionalCapabilities> list) {
        list.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().setAs4BytesCapability(
            new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(58L)).build()).build()).build());
        list.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(
            CParameters1.class, new CParameters1Builder().setGracefulRestartCapability(
                new GracefulRestartCapabilityBuilder().build()).build()).build()).build());
    }
}
