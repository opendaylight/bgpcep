/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BGPPeerProviderTest {

    private BGPPeerProvider peerProvider;
    private final Neighbor neighbor = createNeighbor();
    private final DataBroker dataBroker = Mockito.mock(DataBroker.class);
    private final BGPConfigHolder<Global> globalState = Mockito.mock(BGPConfigHolder.class);
    private final ReadWriteTransaction rwTx = Mockito.mock(ReadWriteTransaction.class);
    private final BGPConfigHolder<Neighbor> neighborState = Mockito.mock(BGPConfigHolder.class);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final BGPConfigStateStore configHolders = Mockito.mock(BGPConfigStateStore.class);
        final BGPConfigModuleProvider moduleProvider = new BGPConfigModuleProvider();

        Mockito.doReturn(this.globalState).when(configHolders).getBGPConfigHolder(Global.class);
        Mockito.doReturn(this.neighborState).when(configHolders).getBGPConfigHolder(Neighbor.class);

        final ModuleKey mKey = Mockito.mock(ModuleKey.class);
        Mockito.doReturn(mKey).when(this.neighborState).getModuleKey(this.neighbor.getKey());
        Mockito.doReturn("mKey").when(mKey).toString();
        Mockito.doReturn(Boolean.TRUE).when(this.neighborState).remove(mKey, createNeighbor());
        this.peerProvider = new BGPPeerProvider(configHolders, moduleProvider, this.dataBroker);

        final CheckedFuture future = Mockito.mock(CheckedFuture.class);
        final Optional<Module> moduleOpt = Mockito.mock(Optional.class);
        Mockito.doReturn(this.rwTx).when(this.dataBroker).newReadWriteTransaction();
        Mockito.doReturn(future).when(this.rwTx).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(moduleOpt).when(future).get();
        Mockito.doReturn(null).when(future).checkedGet();
        Mockito.doReturn(Boolean.TRUE).when(moduleOpt).isPresent();
        final Module module = Mockito.mock(Module.class);
        Mockito.doReturn(module).when(moduleOpt).get();

        Mockito.doReturn(future).when(this.rwTx).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doNothing().when(this.rwTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(future).when(this.rwTx).submit();
        Mockito.doNothing().when(future).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doReturn(Boolean.TRUE).when(this.neighborState).addOrUpdate(Mockito.any(ModuleKey.class), Mockito.any(Identifier.class), Mockito.any(Neighbor.class));
    }

    @Test
    public void testRemoved() {
        this.peerProvider.onNeighborRemoved(this.neighbor);
        Mockito.verify(this.neighborState).remove(Mockito.any(ModuleKey.class), Mockito.any(Neighbor.class));
        Mockito.verify(this.rwTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
    }

    @Test
    public void testModified() {
//        TODO: problem with advertizedTablesFuture on line 98, it won't run and finish successfuly
//        so advertizedTablesFuture.get() on line 103 is blocked
//        same in BGPRibImplProviderTest
    }

    private Neighbor createNeighbor() {
        final List<AfiSafi> families = new ArrayList<AfiSafi>();
        final AfiSafi afi = new AfiSafiBuilder().build();
        families.add(afi);
        return new NeighborBuilder()
            .setKey(new NeighborKey(new IpAddress(new Ipv4Address("1.1.1.5"))))
            .setAfiSafis(new AfiSafisBuilder().setAfiSafi(families).build()).build();
    }
}
