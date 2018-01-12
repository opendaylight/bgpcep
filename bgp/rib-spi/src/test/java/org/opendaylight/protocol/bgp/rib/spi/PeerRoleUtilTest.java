/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;

public class PeerRoleUtilTest {
    @Test
    public void roleForString() {
        assertEquals("ebgp", PeerRoleUtil.roleForString(PeerRole.Ebgp));
        assertEquals("ibgp", PeerRoleUtil.roleForString(PeerRole.Ibgp));
        assertEquals("rr-client", PeerRoleUtil.roleForString(PeerRole.RrClient));
        assertEquals("internal", PeerRoleUtil.roleForString(PeerRole.Internal));
    }
}