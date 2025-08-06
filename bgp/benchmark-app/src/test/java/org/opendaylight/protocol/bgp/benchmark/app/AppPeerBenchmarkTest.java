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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.util.CheckTestUtil.checkEquals;
import static org.opendaylight.protocol.util.CheckTestUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckTestUtil.readDataConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.AddPrefixInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.DeletePrefixInputBuilder;
import org.opendaylight.yangtools.binding.Rpc;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AppPeerBenchmarkTest extends AbstractConcurrentDataBrokerTest {
    private static final String PREFIX = "1.1.1.1/32";
    private static final String NH = "127.0.0.1";
    private static final String PEER_RIB_ID = "app-peer";

    @Mock
    private RpcProviderService rpcRegistry;
    @Mock
    private Registration registration;


    @Before
    public void setUp() {
        doReturn(registration).when(rpcRegistry).registerRpcImplementations(any(Rpc[].class));
        doNothing().when(registration).close();
    }

    @Test
    public void testRpcs() throws Exception {
        try (var appPeerBenchmark = new AppPeerBenchmark(getDataBroker(), rpcRegistry, PEER_RIB_ID)) {
            appPeerBenchmark.start();
            final var routesIID = appPeerBenchmark.getIpv4RoutesIID();

            final var addRpcResult = appPeerBenchmark.addPrefix(new AddPrefixInputBuilder()
                .setBatchsize(Uint32.ONE)
                .setCount(Uint32.ONE)
                .setNexthop(new Ipv4AddressNoZone(NH))
                .setPrefix(new Ipv4Prefix(PREFIX))
                .build()).get();
            final var addResult = addRpcResult.getResult().getResult();
            checkEquals(() -> assertEquals(1, addResult.getCount().intValue()));
            checkEquals(() -> assertEquals(1, addResult.getRate().intValue()));

            readDataConfiguration(getDataBroker(), routesIID, routes -> {
                assertNotNull(routes.getIpv4Route());
                assertEquals(1, routes.getIpv4Route().size());
                return routes;
            });

            final var deleteRpcResult = appPeerBenchmark.deletePrefix(new DeletePrefixInputBuilder()
                .setBatchsize(Uint32.ONE)
                .setCount(Uint32.ONE)
                .setPrefix(new Ipv4Prefix(PREFIX))
                .build()).get();
            final var deleteResult = deleteRpcResult.getResult().getResult();
            checkEquals(() -> assertEquals(1, deleteResult.getCount().intValue()));
            checkEquals(() -> assertEquals(1, deleteResult.getRate().intValue()));

            checkNotPresentConfiguration(getDataBroker(), routesIID);
        }
    }
}
