/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl;

import static org.junit.Assert.assertTrue;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig.BGPOpenConfigListener;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPRibInstanceConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BGPOpenConfigTest {

    private static final BGPOpenConfig CONFIG = new BGPOpenConfig();

    private final ConsumerContext session = Mockito.mock(ConsumerContext.class);
    private final DataBroker dataBroker = Mockito.mock(DataBroker.class);
    private final MountPointService mountService = Mockito.mock(MountPointService.class);
    private final ListenerRegistration<?> registration = Mockito.mock(ListenerRegistration.class);

    private final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
    private final WriteTransaction writeTx = Mockito.mock(WriteTransaction.class);
    private final ListenerRegistration<BGPOpenConfigListener> listener = Mockito.mock(ListenerRegistration.class);

    private Collection<DataTreeModification<Node>> changes;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        Mockito.doNothing().when(this.registration).close();

        Mockito.doReturn(this.dataBroker).when(this.session).getSALService(DataBroker.class);
        Mockito.doReturn(this.mountService).when(this.session).getSALService(MountPointService.class);
        Mockito.doReturn(this.registration).when(this.dataBroker).registerDataTreeChangeListener(Mockito.any(DataTreeIdentifier.class), Mockito.eq(CONFIG));
        Mockito.doReturn(this.txChain).when(this.dataBroker).createTransactionChain(Mockito.any(TransactionChainListener.class));
        Mockito.doReturn(this.writeTx).when(this.txChain).newWriteOnlyTransaction();
        Mockito.doNothing().when(this.txChain).close();
        Mockito.doNothing().when(this.writeTx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class));
        Mockito.doNothing().when(this.writeTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        final CheckedFuture<?, ?> checkedFuture = Mockito.mock(CheckedFuture.class);
        Mockito.doReturn(checkedFuture).when(this.writeTx).submit();
        Mockito.doReturn(null).when(checkedFuture).checkedGet();

        final DataTreeModification<Node> modif1 = Mockito.mock(DataTreeModification.class, Mockito.RETURNS_DEEP_STUBS);
        final DataTreeModification<Node> modif2 = Mockito.mock(DataTreeModification.class, Mockito.RETURNS_DEEP_STUBS);
        final DataTreeModification<Node> modif3 = Mockito.mock(DataTreeModification.class, Mockito.RETURNS_DEEP_STUBS);

        final DataObjectModification<Node> obj1 = Mockito.mock(DataObjectModification.class);
        final DataObjectModification<Node> obj2 = Mockito.mock(DataObjectModification.class);
        final DataObjectModification<Node> obj3 = Mockito.mock(DataObjectModification.class);

        Mockito.doReturn(obj1).when(modif1).getRootNode();
        Mockito.doReturn(obj2).when(modif2).getRootNode();
        Mockito.doReturn(obj3).when(modif3).getRootNode();

        Mockito.doReturn(ModificationType.DELETE).when(obj1).getModificationType();
        Mockito.doReturn(ModificationType.SUBTREE_MODIFIED).when(obj2).getModificationType();
        Mockito.doReturn(ModificationType.WRITE).when(obj3).getModificationType();

        final InstanceIdentifier<Topology> netconfTopo = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()))).build();
        final InstanceIdentifier<Node> iid = netconfTopo.child(Node.class, new NodeKey(new NodeId("controller-config")));

        final DataTreeIdentifier<Node> dtId = new DataTreeIdentifier<Node>(LogicalDatastoreType.CONFIGURATION, iid);
        Mockito.when(modif1.getRootPath()).thenReturn(dtId);
        Mockito.when(modif2.getRootPath()).thenReturn(dtId);
        Mockito.when(modif3.getRootPath()).thenReturn(dtId);
        this.changes = Arrays.asList(modif1, modif2, modif3);

        Mockito.doReturn(this.listener).when(this.dataBroker).registerDataTreeChangeListener(Mockito.any(DataTreeIdentifier.class), Mockito.any(DataTreeChangeListener.class));
        Mockito.doNothing().when(this.listener).close();

        CONFIG.onSessionInitialized(this.session);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInit() {
        Mockito.verify(this.session, Mockito.times(2)).getSALService(Mockito.any(Class.class));
        Mockito.verify(this.dataBroker).registerDataTreeChangeListener(Mockito.any(DataTreeIdentifier.class), Mockito.eq(CONFIG));
    }

    @Test
    public void testGetMapper() {
        assertTrue(CONFIG.getOpenConfigMapper(BGPRibInstanceConfiguration.class).toString().contains("BGPGlobalProviderImpl"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataTreeChanged() {
        CONFIG.onDataTreeChanged(this.changes);
        Mockito.verify(this.dataBroker, Mockito.times(3)).registerDataTreeChangeListener(Mockito.any(DataTreeIdentifier.class), Mockito.any(DataTreeChangeListener.class));
    }

    @After
    public void close() {
        CONFIG.close();
    }
}