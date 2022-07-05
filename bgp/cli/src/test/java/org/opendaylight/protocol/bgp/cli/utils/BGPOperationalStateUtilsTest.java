/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.cli.utils.BGPOperationalStateUtils.PROTOCOLS_IID;
import static org.opendaylight.protocol.bgp.cli.utils.NeighborStateCliUtilsTest.NEIGHBOR_ADDRESS;

import com.google.common.io.Resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.BgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.NeighborsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NetworkInstanceProtocol;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;

public class BGPOperationalStateUtilsTest extends AbstractConcurrentDataBrokerTest {
    static final String RIB_ID = "test-rib";
    private static final String RIB_NOT_FOUND = "RIB not found for [test-rib]\n";
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(output);

    @Test
    public void testDisplayBgpOperationalStateNotFound() {
        BGPOperationalStateUtils.displayBgpOperationalState(getDataBroker(), stream, RIB_ID, null, null);
        assertEquals(RIB_NOT_FOUND, output.toString());
    }

    @Test
    public void testDisplayBgpOperationalStateFound() throws IOException, ExecutionException, InterruptedException {
        createDefaultProtocol();
        BGPOperationalStateUtils.displayBgpOperationalState(getDataBroker(), stream, RIB_ID, null, null);
        final String expected = Resources.toString(getClass().getClassLoader().getResource("global.txt"),
            StandardCharsets.UTF_8);
        assertEquals(expected, output.toString());
    }

    private void createDefaultProtocol() throws ExecutionException, InterruptedException {
        final WriteTransaction wt = getDataBroker().newWriteOnlyTransaction();
        final Bgp bgp = new BgpBuilder()
                .setGlobal(GlobalStateCliUtilsTest.buildGlobal(true).build())
                .setNeighbors(new NeighborsBuilder().setNeighbor(
                        BindingMap.of(NeighborStateCliUtilsTest.createBasicNeighbor())).build())
                .build();
        GlobalStateCliUtilsTest.buildGlobal(true);
        final InstanceIdentifier<Bgp> bgpIID = PROTOCOLS_IID
                .child(Protocol.class, new ProtocolKey(BGP.VALUE, RIB_ID))
                .augmentation(NetworkInstanceProtocol.class).child(Bgp.class);
        wt.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, bgpIID, bgp);
        wt.commit().get();
    }

    @Test
    public void testDisplayNeighborOperationalState() throws IOException, ExecutionException, InterruptedException {
        createDefaultProtocol();
        BGPOperationalStateUtils.displayBgpOperationalState(getDataBroker(), stream, RIB_ID, null,
                NEIGHBOR_ADDRESS);
        final String expected = Resources.toString(getClass().getClassLoader().getResource("empty-neighbor.txt"),
            StandardCharsets.UTF_8);
        assertEquals(expected, output.toString());
    }
}