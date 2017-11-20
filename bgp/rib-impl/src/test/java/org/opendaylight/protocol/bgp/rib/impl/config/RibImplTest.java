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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Shorts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.osgi.framework.ServiceRegistration;

public class RibImplTest extends AbstractConfig {
    private static final List<AfiSafi> AFISAFIS = new ArrayList<>();
    private static final Long ALL_PATHS = 0L;
    private static final BgpTableType TABLE_TYPE = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private static final Ipv4Address BGP_ID = new BgpId(new Ipv4Address("127.0.0.1"));

    static {
        AFISAFIS.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setReceive(true).setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
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
    private ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    @Mock
    private ListenerRegistration<?> dataTreeRegistration;
    @Mock
    private RIBSupport ribSupport;
    @Mock
    private ServiceRegistration<?> serviceRegistration;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Mockito.doReturn(mock(GeneratedClassLoadingStrategy.class)).when(this.extension).getClassLoadingStrategy();
        Mockito.doReturn(this.ribSupport).when(this.extension).getRIBSupport(any(TablesKey.class));
        final NodeIdentifier nii = new NodeIdentifier(QName.create("test").intern());
        Mockito.doReturn(nii).when(this.ribSupport).routeAttributesIdentifier();
        Mockito.doReturn(ImmutableSet.of()).when(this.ribSupport).cacheableAttributeObjects();
        final ChoiceNode choiceNode = mock(ChoiceNode.class);
        Mockito.doReturn(choiceNode).when(this.ribSupport).emptyRoutes();
        Mockito.doReturn(nii).when(choiceNode).getIdentifier();
        Mockito.doReturn(QName.create("test").intern()).when(choiceNode).getNodeType();
        Mockito.doReturn(this.domTx).when(this.domDataBroker).createTransactionChain(any());
        final DOMDataTreeChangeService dOMDataTreeChangeService = mock(DOMDataTreeChangeService.class);
        Mockito.doReturn(Collections.singletonMap(DOMDataTreeChangeService.class, dOMDataTreeChangeService))
            .when(this.domDataBroker).getSupportedExtensions();
        Mockito.doReturn(this.dataTreeRegistration).when(this.domSchemaService).registerSchemaContextListener(any());
        Mockito.doNothing().when(this.dataTreeRegistration).close();
        Mockito.doReturn(mock(ListenerRegistration.class)).when(dOMDataTreeChangeService).registerDataTreeChangeListener(any(), any());
        Mockito.doNothing().when(this.serviceRegistration).unregister();
    }

    @Test
    public void testRibImpl() throws Exception {
        final RibImpl ribImpl = new RibImpl(this.clusterSingletonServiceProvider, this.extension, this.dispatcher,
            this.bindingCodecTreeFactory, this.domDataBroker, this.domSchemaService);
        ribImpl.setServiceRegistration(this.serviceRegistration);
        ribImpl.start(createGlobal(), "rib-test", this.tableTypeRegistry);
        verify(this.extension).getClassLoadingStrategy();
        verify(this.domDataBroker).getSupportedExtensions();
        verify(this.domSchemaService).registerSchemaContextListener(any(RIBImpl.class));
        assertEquals("RIBImpl{}", ribImpl.toString());
        assertEquals(ServiceGroupIdentifier.create("rib-test-service-group"), ribImpl.getRibIServiceGroupIdentifier());
        assertEquals(Collections.singleton(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)), ribImpl.getLocalTablesKeys());
        assertNotNull(ribImpl.getImportPolicyPeerTracker());
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