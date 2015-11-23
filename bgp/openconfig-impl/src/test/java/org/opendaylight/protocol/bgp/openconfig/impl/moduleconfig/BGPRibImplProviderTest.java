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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BGPRibImplProviderTest {

    private BGPRibImplProvider ribProvider;
    private final BGPConfigModuleProvider moduleProvider = new BGPConfigModuleProvider();

    private final ModuleKey moduleKey = Mockito.mock(ModuleKey.class);
    private final DataBroker dataBroker = Mockito.mock(DataBroker.class);
    private final DataBroker dataBroker2 = Mockito.mock(DataBroker.class);
    private final ReadOnlyTransaction rTx = Mockito.mock(ReadOnlyTransaction.class);
    private final ReadOnlyTransaction rTx2 = Mockito.mock(ReadOnlyTransaction.class);
    private final WriteTransaction wTx = Mockito.mock(WriteTransaction.class);


    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final BGPConfigStateStore configHolders = Mockito.mock(BGPConfigStateStore.class);

        final BGPConfigHolder<Global> globalState = Mockito.mock(BGPConfigHolder.class);
        Mockito.doReturn(globalState).when(configHolders).getBGPConfigHolder(Global.class);

        this.ribProvider = new BGPRibImplProvider(configHolders, this.moduleProvider);

        Mockito.doReturn(this.moduleKey).when(globalState).getModuleKey(Mockito.any(GlobalIdentifier.class));
        Mockito.doReturn("module-key").when(this.moduleKey).toString();
        Mockito.doReturn(Boolean.TRUE).when(globalState).remove(this.moduleKey);
        Mockito.doReturn(Boolean.TRUE).when(globalState).addOrUpdate(Mockito.any(ModuleKey.class), Mockito.any(GlobalIdentifier.class), Mockito.any(Global.class));

        Mockito.doReturn(this.rTx).when(this.dataBroker).newReadOnlyTransaction();
        Mockito.doReturn(this.rTx2).when(this.dataBroker2).newReadOnlyTransaction();

        Mockito.doReturn(this.wTx).when(this.dataBroker).newWriteOnlyTransaction();
        final CheckedFuture<Void, Exception> future = Mockito.mock(CheckedFuture.class);
        final CheckedFuture<Void, Exception> future2 = Mockito.mock(CheckedFuture.class);
        Mockito.doReturn(future).when(this.rTx).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(future2).when(this.rTx2).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));

        Mockito.doNothing().when(this.wTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(future).when(this.wTx).submit();
        Mockito.doReturn(null).when(future).checkedGet();

        final Optional<Module> moduleOpt = Mockito.mock(Optional.class);
        final Optional<Module> moduleOpt2 = Mockito.mock(Optional.class);
        Mockito.doReturn(moduleOpt).when(future).get();
        Mockito.doReturn(moduleOpt2).when(future2).get();
        Mockito.doReturn(Boolean.TRUE).when(moduleOpt).isPresent();
        Mockito.doReturn(Boolean.FALSE).when(moduleOpt2).isPresent();
        final Module module = Mockito.mock(Module.class);
        Mockito.doReturn(module).when(moduleOpt).get();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGlobalRemoved() throws TransactionCommitFailedException {
        this.ribProvider.onGlobalRemoved(null, this.dataBroker);
        Mockito.verify(this.rTx).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.verify(this.wTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
    }

    @Test
    public void testGlobalModified() {
        this.ribProvider.onGlobalModified(createGlobal(), this.dataBroker2);
    }

    private Global createGlobal() {
        final GlobalBuilder global = new GlobalBuilder();
        final List<AfiSafi> families = new ArrayList<AfiSafi>();
        global.setAfiSafis(new AfiSafisBuilder().setAfiSafi(families).build())
            .setConfig(new ConfigBuilder()
                .setRouterId(new Ipv4Address("1.1.1.1"))
                .setAs(new AsNumber(10L)).build());
        return global.build();
    }

}
