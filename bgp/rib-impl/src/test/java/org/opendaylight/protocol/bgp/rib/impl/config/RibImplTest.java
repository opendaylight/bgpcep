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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.GlobalAddPathsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.GlobalAddPathsConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.osgi.framework.ServiceRegistration;

public class RibImplTest extends AbstractConfig {
    private static final List<AfiSafi> AFISAFIS = new ArrayList<>();
    private static final BgpTableType TABLE_TYPE = new BgpTableTypeImpl(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    private static final BgpId BGP_ID = new BgpId(new Ipv4AddressNoZone("127.0.0.1"));

    static {
        AFISAFIS.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
                .addAugmentation(GlobalAddPathsConfig.class, new GlobalAddPathsConfigBuilder().setReceive(true)
                        .setSendMax(Uint8.ZERO).build()).build());
    }

    @Mock
    private RIBExtensionConsumerContext extension;
    @Mock
    private BindingCodecTreeFactory bindingCodecTreeFactory;
    @Mock
    private DOMDataBroker domDataBroker;
    @Mock
    private DOMSchemaService domSchemaService;
    @Mock
    private ListenerRegistration<?> dataTreeRegistration;
    @Mock
    private RIBSupport<?, ?, ?, ?> ribSupport;
    @Mock
    private ServiceRegistration<?> serviceRegistration;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        doReturn(mock(GeneratedClassLoadingStrategy.class)).when(this.extension).getClassLoadingStrategy();
        doReturn(this.ribSupport).when(this.extension).getRIBSupport(any(TablesKey.class));
        final NodeIdentifier nii = new NodeIdentifier(QName.create("", "test").intern());
        doReturn(nii).when(this.ribSupport).routeAttributesIdentifier();
        doReturn(ImmutableSet.of()).when(this.ribSupport).cacheableAttributeObjects();
        final MapEntryNode emptyTable = mock(MapEntryNode.class);
        doReturn(emptyTable).when(this.ribSupport).emptyTable();
        final NodeIdentifierWithPredicates niie = NodeIdentifierWithPredicates.of(Rib.QNAME,
                QName.create("", "test").intern(), "t");
        doReturn(niie).when(emptyTable).getIdentifier();
        doReturn(QName.create("", "test").intern()).when(emptyTable).getNodeType();
        doReturn(this.domTx).when(this.domDataBroker).createMergingTransactionChain(any());
        final DOMDataTreeChangeService dOMDataTreeChangeService = mock(DOMDataTreeChangeService.class);
        doReturn(ImmutableClassToInstanceMap.of(DOMDataTreeChangeService.class, dOMDataTreeChangeService))
                .when(this.domDataBroker).getExtensions();
        doReturn(this.dataTreeRegistration).when(this.domSchemaService).registerSchemaContextListener(any());
        doNothing().when(this.dataTreeRegistration).close();
        doReturn(mock(ListenerRegistration.class)).when(dOMDataTreeChangeService)
                .registerDataTreeChangeListener(any(), any());
        doNothing().when(this.serviceRegistration).unregister();
    }

    @Test
    public void testRibImpl() {
        final RibImpl ribImpl = new RibImpl(
                this.extension,
                this.dispatcher,
                this.policyProvider,
                this.bindingCodecTreeFactory,
                this.domDataBroker,
                getDataBroker(),
                this.domSchemaService);
        ribImpl.setServiceRegistration(this.serviceRegistration);
        ribImpl.start(createGlobal(), "rib-test", this.tableTypeRegistry);
        verify(this.extension).getClassLoadingStrategy();
        verify(this.domDataBroker).getExtensions();
        verify(this.domSchemaService).registerSchemaContextListener(any(RIBImpl.class));
        assertEquals("RIBImpl{bgpId=Ipv4Address{_value=127.0.0.1}, localTables=[BgpTableTypeImpl ["
                + "getAfi()=interface org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types."
                + "rev180329.Ipv4AddressFamily, "
                + "getSafi()=interface org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types."
                + "rev180329.UnicastSubsequentAddressFamily]]}", ribImpl.toString());
        assertEquals(Collections.singleton(new TablesKey(Ipv4AddressFamily.class,
                UnicastSubsequentAddressFamily.class)), ribImpl.getLocalTablesKeys());
        assertNotNull(ribImpl.getService());
        assertNotNull(ribImpl.getInstanceIdentifier());
        assertEquals(AS, ribImpl.getLocalAs());
        assertEquals(BGP_ID, ribImpl.getBgpIdentifier());
        assertEquals(Collections.singleton(TABLE_TYPE), ribImpl.getLocalTables());
        assertEquals(this.dispatcher, ribImpl.getDispatcher());
        assertEquals(this.extension, ribImpl.getRibExtensions());
        assertNotNull(ribImpl.getRibSupportContext());
        assertNotNull(ribImpl.getCodecsRegistry());

        ribImpl.close();
        verify(this.dataTreeRegistration).close();
        verify(this.dataTreeRegistration).close();
        verify(this.serviceRegistration).unregister();
    }

    private static Global createGlobal() {
        return new GlobalBuilder()
                .setAfiSafis(new AfiSafisBuilder().setAfiSafi(AFISAFIS).build())
                .setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base
                        .ConfigBuilder().setAs(AS).setRouterId(BGP_ID).build()).build();
    }
}