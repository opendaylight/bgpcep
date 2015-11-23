/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.Config1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.Config1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class BGPOpenConfigListenerTest {

    private BGPOpenConfigListener configListener;

    private Collection<DataTreeModification<Bgp>> changes;
    private final DataObjectModification<Bgp> rootNode = Mockito.mock(DataObjectModification.class);
    final DataObjectModification<DataObject> modif1 = Mockito.mock(DataObjectModification.class);
    final DataObjectModification<DataObject> modif2 = Mockito.mock(DataObjectModification.class);
    final DataObjectModification<DataObject> modif3 = Mockito.mock(DataObjectModification.class);

    final DataObjectModification<DataObject> child1 = Mockito.mock(DataObjectModification.class);
    final DataObjectModification<DataObject> child2 = Mockito.mock(DataObjectModification.class);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final DataBroker dataBroker = Mockito.mock(DataBroker.class);
        final ListenerRegistration<BGPOpenConfigListener> regListener = Mockito.mock(ListenerRegistration.class);

        Mockito.doReturn(regListener).when(dataBroker).registerDataTreeChangeListener(Mockito.any(DataTreeIdentifier.class), Mockito.any(BGPOpenConfigListener.class));
        Mockito.doNothing().when(regListener).close();

        final Callable<MountPoint>  callable = Mockito.mock(Callable.class);
        final MountPoint mountPoint = Mockito.mock(MountPoint.class);
        final Optional<DataBroker> dataBrokerOpt = Mockito.mock(Optional.class);

        Mockito.doReturn(mountPoint).when(callable).call();
        Mockito.doReturn(dataBrokerOpt).when(mountPoint).getService(DataBroker.class);
        Mockito.doReturn(Boolean.TRUE).when(dataBrokerOpt).isPresent();
        Mockito.doReturn(dataBroker).when(dataBrokerOpt).get();

        final BGPConfigStateStore configStateHolders = Mockito.mock(BGPConfigStateStore.class);
        final BGPConfigHolder<Global> globalState = Mockito.mock(BGPConfigHolder.class);
        final BGPConfigHolder<Neighbor> neighborState = Mockito.mock(BGPConfigHolder.class);

        Mockito.doReturn(globalState).when(configStateHolders).getBGPConfigHolder(Global.class);
        Mockito.doReturn(neighborState).when(configStateHolders).getBGPConfigHolder(Neighbor.class);
        Mockito.doReturn(null).when(globalState).getModuleKey(Mockito.any(GlobalIdentifier.class));
        Mockito.doReturn(null).when(neighborState).getModuleKey(Mockito.any(GlobalIdentifier.class));

        this.configListener = new BGPOpenConfigListener(dataBroker, callable, configStateHolders);
        final DataTreeModification<Bgp> modification = Mockito.mock(DataTreeModification.class);
        Mockito.doReturn(this.rootNode).when(modification).getRootNode();

        final Collection<DataObjectModification<DataObject>> children = Arrays.asList(this.modif1, this.modif2, this.modif3);

        Mockito.doReturn(children).when(this.rootNode).getModifiedChildren();
        Mockito.doReturn(ModificationType.SUBTREE_MODIFIED).when(this.modif1).getModificationType();
        Mockito.doReturn(ModificationType.DELETE).when(this.modif2).getModificationType();
        Mockito.doReturn(ModificationType.DELETE).when(this.modif3).getModificationType();

        final Global global = Mockito.mock(Global.class);
        final Neighbors neighbors = Mockito.mock(Neighbors.class);
        final ArrayList<Neighbor> neighbor = new ArrayList<Neighbor>();
        neighbor.add(createNeighbor());
        Mockito.doReturn(neighbor).when(neighbors).getNeighbor();
        Mockito.doReturn(neighbors).when(this.modif1).getDataAfter();
        Mockito.doReturn(global).when(this.modif2).getDataBefore();
        Mockito.doReturn(neighbors).when(this.modif3).getDataBefore();

        final Collection<DataObjectModification<DataObject>> children2 = Arrays.asList(this.child1, this.child2);
        Mockito.doReturn(ModificationType.DELETE).when(this.child1).getModificationType();
        Mockito.doReturn(createNeighbor()).when(this.child1).getDataBefore();
        Mockito.doReturn(createNeighbor()).when(this.child2).getDataAfter();
        Mockito.doReturn(ModificationType.SUBTREE_MODIFIED).when(this.child2).getModificationType();
        Mockito.doReturn(children2).when(this.modif1).getModifiedChildren();
        Mockito.doReturn(null).when(dataBroker).newReadOnlyTransaction();

        this.changes = Arrays.asList(modification);
    }

    @Test
    public void testOnDataTreeChanged() {
        this.configListener.onDataTreeChanged(this.changes);
        Mockito.verify(this.rootNode).getModifiedChildren();
        Mockito.verify(this.modif1).getModificationType();
        Mockito.verify(this.modif2).getModificationType();
        Mockito.verify(this.modif3).getModificationType();
        Mockito.verify(this.modif1).getDataAfter();
        Mockito.verify(this.modif2).getDataBefore();
        Mockito.verify(this.modif3).getDataBefore();
        Mockito.verify(this.child1).getDataBefore();
        Mockito.verify(this.child2).getDataAfter();
    }

    @After
    public void after() {
        this.configListener.close();
    }

    private Neighbor createNeighbor() {
        final NeighborBuilder neighbor = new NeighborBuilder();
        neighbor.setConfig(new ConfigBuilder()
            .addAugmentation(Config1.class, new Config1Builder()
                .setPeerGroup(OpenConfigUtil.APPLICATION_PEER_GROUP_NAME)
                .build())
            .build());
        return neighbor.build();
    }

}
