/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.benchmark.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.util.CheckTestUtil.checkEquals;
import static org.opendaylight.protocol.util.CheckTestUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckTestUtil.readDataConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.AddPrefixInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.AddPrefixInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.AddPrefixOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.DeletePrefixInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.DeletePrefixInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.DeletePrefixOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.OdlBgpAppPeerBenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.output.Result;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

public class AppPeerBenchmarkTest extends AbstractConcurrentDataBrokerTest {

    private static final String PREFIX = "1.1.1.1/32";
    private static final String NH = "127.0.0.1";
    private static final String PEER_RIB_ID = "app-peer";

    @Mock
    private RpcProviderService rpcRegistry;
    @Mock
    private ObjectRegistration<OdlBgpAppPeerBenchmarkService> registration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(this.registration).when(this.rpcRegistry).registerRpcImplementation(Mockito.any(),
                Mockito.any(OdlBgpAppPeerBenchmarkService.class));
        doNothing().when(this.registration).close();
    }

    @Test
    public void testRpcs() throws Exception {
        final AppPeerBenchmark appPeerBenchmark = new AppPeerBenchmark(getDataBroker(), this.rpcRegistry, PEER_RIB_ID);
        appPeerBenchmark.start();
        final InstanceIdentifier<Ipv4Routes> routesIID = appPeerBenchmark.getIpv4RoutesIID();

        final AddPrefixInput addPrefix = new AddPrefixInputBuilder().setBatchsize(Uint32.ONE).setCount(Uint32.ONE)
                .setNexthop(new Ipv4AddressNoZone(NH)).setPrefix(new Ipv4Prefix(PREFIX)).build();

        final RpcResult<AddPrefixOutput> addRpcResult = appPeerBenchmark.addPrefix(addPrefix).get();
        final Result addResult = addRpcResult.getResult().getResult();
        checkEquals(() -> assertEquals(1, addResult.getCount().intValue()));
        checkEquals(() -> assertEquals(1, addResult.getRate().intValue()));

        readDataConfiguration(getDataBroker(), routesIID, routes -> {
            assertNotNull(routes.getIpv4Route());
            assertEquals(1, routes.getIpv4Route().size());
            return routes;
        });

        final DeletePrefixInput deletePrefix = new DeletePrefixInputBuilder().setBatchsize(Uint32.ONE)
                .setCount(Uint32.ONE).setPrefix(new Ipv4Prefix(PREFIX)).build();
        final RpcResult<DeletePrefixOutput> deleteRpcResult = appPeerBenchmark
                .deletePrefix(deletePrefix).get();
        final Result deleteResult = deleteRpcResult.getResult().getResult();
        checkEquals(() -> assertEquals(1, deleteResult.getCount().intValue()));
        checkEquals(() -> assertEquals(1, deleteResult.getRate().intValue()));

        checkNotPresentConfiguration(getDataBroker(), appPeerBenchmark.getIpv4RoutesIID());

        appPeerBenchmark.close();
    }
}
