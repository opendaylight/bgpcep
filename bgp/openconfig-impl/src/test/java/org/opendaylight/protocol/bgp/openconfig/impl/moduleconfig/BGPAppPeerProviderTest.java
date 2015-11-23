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
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpApplicationPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.application.peer.TargetRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BGPAppPeerProviderTest {

    private BGPAppPeerProvider appPeerProvider;
    private final Neighbor neighbor = new NeighborBuilder().setKey(new NeighborKey(new IpAddress(new Ipv4Address("1.1.1.2")))).build();
    private final DataBroker dataBroker = Mockito.mock(DataBroker.class);
    private final BGPConfigHolder<Global> globalState = Mockito.mock(BGPConfigHolder.class);
    private final WriteTransaction wTx = Mockito.mock(WriteTransaction.class);
    private final ReadWriteTransaction rwTx = Mockito.mock(ReadWriteTransaction.class);
    private final BGPConfigHolder<Neighbor> neighborState = Mockito.mock(BGPConfigHolder.class);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final BGPConfigStateStore configHolders = Mockito.mock(BGPConfigStateStore.class);
        final BGPConfigModuleProvider moduleProvider = new BGPConfigModuleProvider();
        final ModuleKey mKey = Mockito.mock(ModuleKey.class);

        Mockito.doReturn(this.globalState).when(configHolders).getBGPConfigHolder(Global.class);
        Mockito.doReturn(this.neighborState).when(configHolders).getBGPConfigHolder(Neighbor.class);
        Mockito.doReturn(mKey).when(this.neighborState).getModuleKey(this.neighbor.getKey());
        Mockito.doReturn("mKey").when(mKey).toString();
        Mockito.doReturn(Boolean.TRUE).when(this.neighborState).remove(Mockito.any(ModuleKey.class), Mockito.any(Neighbor.class));
        Mockito.doReturn(Boolean.TRUE).when(this.neighborState).addOrUpdate(Mockito.any(ModuleKey.class), Mockito.any(Identifier.class), Mockito.any(Neighbor.class));

        this.appPeerProvider = new BGPAppPeerProvider(configHolders, moduleProvider, this.dataBroker);

        final ReadOnlyTransaction rTx = Mockito.mock(ReadOnlyTransaction.class);
        final CheckedFuture future = Mockito.mock(CheckedFuture.class);
        final Optional<Module> moduleOpt = Mockito.mock(Optional.class);
        Mockito.doReturn(rTx).when(this.dataBroker).newReadOnlyTransaction();
        Mockito.doReturn(this.wTx).when(this.dataBroker).newWriteOnlyTransaction();
        Mockito.doReturn(this.rwTx).when(this.dataBroker).newReadWriteTransaction();
        Mockito.doReturn(future).when(rTx).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(future).when(this.rwTx).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doNothing().when(this.rwTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(future).when(this.wTx).submit();
        Mockito.doReturn(future).when(this.rwTx).submit();
        Mockito.doNothing().when(this.wTx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(Module.class));
        Mockito.doReturn(moduleOpt).when(future).get();
        Mockito.doReturn(null).when(future).checkedGet();
        Mockito.doNothing().when(future).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doReturn(Boolean.TRUE).when(moduleOpt).isPresent();
        final Module module = Mockito.mock(Module.class);
        Mockito.doReturn(module).when(moduleOpt).get();
        final BgpApplicationPeer appPeer = Mockito.mock(BgpApplicationPeer.class);
        Mockito.doReturn(appPeer).when(module).getConfiguration();
        final TargetRib rib = Mockito.mock(TargetRib.class);
        Mockito.doReturn(rib).when(appPeer).getTargetRib();
        final ApplicationRibId appRib = Mockito.mock(ApplicationRibId.class);
        Mockito.doReturn(appRib).when(appPeer).getApplicationRibId();
        Mockito.doReturn("appRib").when(appRib).toString();
        Mockito.doReturn("targetRib").when(rib).toString();
    }

    @Test
    public void testRemove() {
        this.appPeerProvider.onNeighborRemoved(this.neighbor);
        Mockito.verify(this.neighborState).remove(Mockito.any(ModuleKey.class), Mockito.any(Neighbor.class));
        Mockito.verify(this.rwTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
    }

    @Test
    public void testModified() {
        this.appPeerProvider.onNeighborModified(this.neighbor);
        Mockito.verify(this.neighborState).addOrUpdate(Mockito.any(ModuleKey.class), Mockito.any(Identifier.class), Mockito.any(Neighbor.class));
    }

}
