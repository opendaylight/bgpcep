/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPStateCollector;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.GlobalAddPathsConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public class RibImplTest extends AbstractConfig {
    private static final Map<AfiSafiKey, AfiSafi> AFISAFIS = BindingMap.of(new AfiSafiBuilder()
        .setAfiSafiName(IPV4UNICAST.class)
        .addAugmentation(new GlobalAddPathsConfigBuilder().setReceive(true).setSendMax(Uint8.ZERO).build())
        .build());
    private static final BgpTableType TABLE_TYPE = new BgpTableTypeImpl(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    private static final BgpId BGP_ID = new BgpId(new Ipv4AddressNoZone("127.0.0.1"));

    @Mock
    private RIBExtensionConsumerContext extension;
    @Mock
    private CodecsRegistry codecsRegistry;
    @Mock
    private DOMDataBroker domDataBroker;
    @Mock
    private RIBSupport<?, ?> ribSupport;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        doReturn(ribSupport).when(extension).getRIBSupport(any(TablesKey.class));
        final NodeIdentifier nii = new NodeIdentifier(QName.create("", "test").intern());
        doReturn(nii).when(ribSupport).routeAttributesIdentifier();
        doReturn(ImmutableSet.of()).when(ribSupport).cacheableAttributeObjects();
        final MapEntryNode emptyTable = mock(MapEntryNode.class);
        doReturn(emptyTable).when(ribSupport).emptyTable();
        final NodeIdentifierWithPredicates niie = NodeIdentifierWithPredicates.of(Rib.QNAME,
                QName.create("", "test").intern(), "t");
        doReturn(niie).when(emptyTable).getIdentifier();
        doReturn(domTx).when(domDataBroker).createMergingTransactionChain(any());
        final DOMDataTreeChangeService dOMDataTreeChangeService = mock(DOMDataTreeChangeService.class);
        doReturn(ImmutableClassToInstanceMap.of(DOMDataTreeChangeService.class, dOMDataTreeChangeService))
                .when(domDataBroker).getExtensions();
        doReturn(mock(ListenerRegistration.class)).when(dOMDataTreeChangeService)
                .registerDataTreeChangeListener(any(), any());
    }

    @Test
    public void testRibImpl() throws ExecutionException, InterruptedException {
        final RibImpl ribImpl = new RibImpl(extension, dispatcher, policyProvider, codecsRegistry,
                new BGPStateCollector(), domDataBroker);
        ribImpl.start(createGlobal(), "rib-test", tableTypeRegistry);
        verify(domDataBroker).getExtensions();
        assertEquals("RIBImpl{bgpId=Ipv4Address{_value=127.0.0.1}, localTables=[BgpTableTypeImpl ["
                + "getAfi()=interface org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types."
                + "rev200120.Ipv4AddressFamily, "
                + "getSafi()=interface org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types."
                + "rev200120.UnicastSubsequentAddressFamily]]}", ribImpl.toString());
        assertEquals(Collections.singleton(new TablesKey(Ipv4AddressFamily.class,
                UnicastSubsequentAddressFamily.class)), ribImpl.getLocalTablesKeys());
        assertNotNull(ribImpl.getService());
        assertNotNull(ribImpl.getInstanceIdentifier());
        assertEquals(AS, ribImpl.getLocalAs());
        assertEquals(BGP_ID, ribImpl.getBgpIdentifier());
        assertEquals(Collections.singleton(TABLE_TYPE), ribImpl.getLocalTables());
        assertEquals(dispatcher, ribImpl.getDispatcher());
        assertEquals(extension, ribImpl.getRibExtensions());
        assertNotNull(ribImpl.getRibSupportContext());
        assertNotNull(ribImpl.getCodecsRegistry());

        ribImpl.stop().get();
    }

    private static Global createGlobal() {
        return new GlobalBuilder()
                .setAfiSafis(new AfiSafisBuilder().setAfiSafi(AFISAFIS).build())
                .setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base
                        .ConfigBuilder().setAs(AS).setRouterId(BGP_ID).build()).build();
    }
}
