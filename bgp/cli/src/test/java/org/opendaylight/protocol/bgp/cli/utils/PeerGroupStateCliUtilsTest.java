/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentationBuilder;

public final class PeerGroupStateCliUtilsTest {

    private static final String TEST_GROUP = "test-group";
    static final String UTF8 = "UTF-8";
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(this.output);

    @Test
    public void testEmptyPeerGroupStateCli() throws IOException {
        final PeerGroupBuilder peerGroup = new PeerGroupBuilder().setPeerGroupName(TEST_GROUP);
        PeerGroupStateCliUtils.displayPeerOperationalState(Collections.singletonList(peerGroup.build()), this.stream);

        final String expected = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("empty-peer-group.txt"), UTF8);
        assertEquals(expected, this.output.toString());
    }

    @Test
    public void testPeerGroupStateCli() throws IOException {
        final PeerGroupBuilder peerGroup = new PeerGroupBuilder().setPeerGroupName(TEST_GROUP);

        final PeerGroupStateAugmentation groupState = new PeerGroupStateAugmentationBuilder()
                .setTotalPrefixes(1L)
                .setTotalPaths(2L)
                .build();

        peerGroup.setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                .StateBuilder().addAugmentation(PeerGroupStateAugmentation.class, groupState).build());
        PeerGroupStateCliUtils.displayPeerOperationalState(Collections.singletonList(peerGroup.build()), this.stream);

        final String expected = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("peer-group.txt"), UTF8);
        assertEquals(expected, this.output.toString());
    }
}