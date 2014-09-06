/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

public class ApiTest {

    @Test
    public void testBGPSessionProposalImpl() {
        final Map<Class<? extends AddressFamily>, Class<? extends SubsequentAddressFamily>> map = new HashMap<>();
        map.put(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);

        final BGPSessionProposalImpl proposal = new BGPSessionProposalImpl((short) 5, new AsNumber(58L), null, map);
        final BGPSessionPreferences sp = proposal.getProposal();
        assertNull(sp.getBgpId());
        assertEquals(proposal.getHoldTimer(), sp.getHoldTime());
        assertEquals(proposal.getAs(), sp.getMyAs());
        assertNull(proposal.getBgpId());

    }
}
