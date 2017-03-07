/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.benchmark.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.util.CheckUtil.readData;

import java.util.concurrent.ExecutionException;
import javax.management.MalformedObjectNameException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.AddPrefixInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.AddPrefixOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.DeletePrefixInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.DeletePrefixOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.OdlBgpAppPeerBenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.output.Result;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class AppPeerBenchmarkTest extends AbstractConcurrentDataBrokerTest {

    private static final String PREFIX = "1.1.1.1/32";
    private static final String NH = "127.0.0.1";
    private static final String PEER_RIB_ID = "app-peer";
    private static final InstanceIdentifier<ApplicationRib> BASE_IID = KeyedInstanceIdentifier
        .builder(ApplicationRib.class, new ApplicationRibKey(new ApplicationRibId(PEER_RIB_ID))).build();
    private static final InstanceIdentifier tablesIId = BASE_IID
        .child(Tables.class, new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
    private static final InstanceIdentifier<Ipv4Routes> ROUTES = tablesIId.child(Ipv4Routes.class);

    @Mock
    private RpcProviderRegistry rpcRegistry;
    @Mock
    private RoutedRpcRegistration<OdlBgpAppPeerBenchmarkService> registration;

    private AppPeerBenchmark appPeerBenchmark;

    @Before
    public void setUp() throws MalformedObjectNameException {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(this.registration).when(this.rpcRegistry).addRpcImplementation(Mockito.any(),
            Mockito.any(OdlBgpAppPeerBenchmarkService.class));
        Mockito.doNothing().when(this.registration).close();
        this.appPeerBenchmark = new AppPeerBenchmark(getDataBroker(), this.rpcRegistry, PEER_RIB_ID);
    }

    @Test
    public void testRpcs() throws InterruptedException, ExecutionException, ReadFailedException {
        final RpcResult<AddPrefixOutput> addRpcResult = this.appPeerBenchmark.addPrefix(new AddPrefixInputBuilder()
            .setBatchsize(1L).setCount(1L).setNexthop(new Ipv4Address(NH))
            .setPrefix(new Ipv4Prefix(PREFIX)).build()).get();
        final Result addResult = addRpcResult.getResult().getResult();
        assertEquals(1, addResult.getCount().intValue());
        assertEquals(1, addResult.getRate().intValue());
        readData(getDataBroker(), ROUTES, routesAfterAdd -> {
            assertEquals(1, routesAfterAdd.getIpv4Route().size());
            return routesAfterAdd;
        });

        final RpcResult<DeletePrefixOutput> deleteRpcResult = this.appPeerBenchmark.deletePrefix(new DeletePrefixInputBuilder()
            .setBatchsize(1L)
            .setCount(1L)
            .setPrefix(new Ipv4Prefix(PREFIX)).build()).get();
        final Result deleteResult = deleteRpcResult.getResult().getResult();
        assertEquals(1, deleteResult.getCount().intValue());
        assertEquals(1, deleteResult.getRate().intValue());
        readData(getDataBroker(), ROUTES, routesAfterDelete -> {
            assertTrue(routesAfterDelete.getIpv4Route().isEmpty());
            return routesAfterDelete;
        });
    }

    @After
    public void tearDown() {
        this.appPeerBenchmark.close();
    }
}
