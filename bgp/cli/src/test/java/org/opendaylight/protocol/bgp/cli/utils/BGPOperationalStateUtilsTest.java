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
import static org.opendaylight.protocol.bgp.cli.utils.PeerGroupStateCliUtilsTest.UTF8;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.BgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.NeighborsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BGPOperationalStateUtilsTest extends AbstractConcurrentDataBrokerTest {
    static final String RIB_ID = "test-rib";
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(this.output);

    @Test
    public void testDisplayBgpOperationalStateNotFound() {
        BGPOperationalStateUtils.displayBgpOperationalState(getDataBroker(), this.stream, RIB_ID, null, null);
        assertEquals("RIB not found for [test-rib]\n", this.output.toString());
    }

    @Test
    @SuppressWarnings("IllegalThrows")
    public void testDisplayBgpOperationalStateFound() throws Exception {
        createDefaultProtocol();
        BGPOperationalStateUtils.displayBgpOperationalState(getDataBroker(), this.stream, RIB_ID, null, null);
        final String expected = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("global.txt"), UTF8);
        assertEquals(expected, this.output.toString());
    }

    private void createDefaultProtocol() throws ExecutionException, InterruptedException {
        final WriteTransaction wt = getDataBroker().newWriteOnlyTransaction();
        final Bgp bgp = new BgpBuilder()
                .setGlobal(GlobalStateCliUtilsTest.buildGlobal(true).build())
                .setNeighbors(new NeighborsBuilder().setNeighbor(
                        Collections.singletonList(NeighborStateCliUtilsTest.createBasicNeighbor())).build())
                .build();
        GlobalStateCliUtilsTest.buildGlobal(true);
        final InstanceIdentifier<Bgp> bgpIID = PROTOCOLS_IID
                .child(Protocol.class, new ProtocolKey(BGP.class, RIB_ID))
                .augmentation(Protocol1.class).child(Bgp.class);
        wt.put(LogicalDatastoreType.OPERATIONAL, bgpIID, bgp, true);
        wt.submit().get();
    }

    @Test
    @SuppressWarnings("IllegalThrows")
    public void testDisplayNeighborOperationalState() throws Exception {
        createDefaultProtocol();
        BGPOperationalStateUtils.displayBgpOperationalState(getDataBroker(), this.stream, RIB_ID, null,
                NEIGHBOR_ADDRESS);
        final String expected = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("empty-neighbor.txt"), UTF8);
        assertEquals(expected, this.output.toString());
    }
}