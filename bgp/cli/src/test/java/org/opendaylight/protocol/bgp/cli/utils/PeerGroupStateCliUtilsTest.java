/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import static org.junit.Assert.assertEquals;

import com.google.common.io.Resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.PeerGroupStateAugmentationBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class PeerGroupStateCliUtilsTest {

    private static final String TEST_GROUP = "test-group";
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(this.output);

    @Test
    public void testEmptyPeerGroupStateCli() throws IOException {
        final PeerGroupBuilder peerGroup = new PeerGroupBuilder().setPeerGroupName(TEST_GROUP);
        PeerGroupStateCliUtils.displayPeerOperationalState(Collections.singletonList(peerGroup.build()), this.stream);

        final String expected = Resources.toString(getClass().getClassLoader().getResource("empty-peer-group.txt"),
            StandardCharsets.UTF_8);
        assertEquals(expected, this.output.toString());
    }

    @Test
    public void testPeerGroupStateCli() throws IOException {
        PeerGroupStateCliUtils.displayPeerOperationalState(Collections.singletonList(new PeerGroupBuilder()
            .setPeerGroupName(TEST_GROUP)
            .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                .StateBuilder()
                    .addAugmentation(new PeerGroupStateAugmentationBuilder()
                        .setTotalPrefixes(Uint32.ONE)
                        .setTotalPaths(Uint32.TWO)
                        .build())
                    .build())
            .build()), this.stream);

        final String expected = Resources.toString(getClass().getClassLoader().getResource("peer-group.txt"),
            StandardCharsets.UTF_8);
        assertEquals(expected, this.output.toString());
    }
}