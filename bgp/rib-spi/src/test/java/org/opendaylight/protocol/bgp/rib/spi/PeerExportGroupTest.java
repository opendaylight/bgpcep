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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class PeerExportGroupTest {
    @Test
    public void defaultPeerExportGroupTest() {
        final  PeerExportGroup.PeerExporTuple peerExportGroup = new PeerExportGroup.PeerExporTuple(YangInstanceIdentifier.EMPTY, PeerRole.Ebgp);
        assertEquals(PeerRole.Ebgp, peerExportGroup.getRole());
        assertEquals(YangInstanceIdentifier.EMPTY, peerExportGroup.getYii());
    }
}
