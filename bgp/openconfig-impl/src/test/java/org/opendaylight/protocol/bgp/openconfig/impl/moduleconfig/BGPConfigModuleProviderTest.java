/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class BGPConfigModuleProviderTest {

    private static final BGPConfigModuleProvider PROVIDER = new BGPConfigModuleProvider();

    private final Module module = Mockito.mock(Module.class);
    private final ModuleKey key = Mockito.mock(ModuleKey.class);
    private final WriteTransaction wtx = Mockito.mock(WriteTransaction.class);
    private final ReadTransaction rtx = Mockito.mock(ReadTransaction.class);

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(this.key).when(this.module).getKey();
        Mockito.doReturn("key").when(this.key).toString();
        Mockito.doReturn("module").when(this.module).toString();
        Mockito.doNothing().when(this.wtx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(KeyedInstanceIdentifier.class), Mockito.any(Module.class));
        Mockito.doNothing().when(this.wtx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(KeyedInstanceIdentifier.class));
        final CheckedFuture<Void, Exception> future = Mockito.mock(CheckedFuture.class);
        Mockito.doReturn(future).when(this.wtx).submit();
        Mockito.doReturn(null).when(future).checkedGet();

        Mockito.doReturn(future).when(this.rtx).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
    }

    @Test
    public void testWriteTransactions() throws TransactionCommitFailedException {
        PROVIDER.putModuleConfiguration(this.module, this.wtx);
        PROVIDER.removeModuleConfiguration(this.key, this.wtx);
        Mockito.verify(this.wtx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(KeyedInstanceIdentifier.class), Mockito.any(Module.class));
        Mockito.verify(this.wtx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(KeyedInstanceIdentifier.class));
        Mockito.verify(this.wtx, Mockito.times(2)).submit();
    }

    @Test
    public void testReadTransactions() throws InterruptedException, ExecutionException {
        PROVIDER.readModuleConfiguration(this.key, this.rtx);
        final ServiceKey sKey = Mockito.mock(ServiceKey.class);
        PROVIDER.readConfigService(sKey, this.rtx);
        Mockito.verify(this.rtx, Mockito.times(2)).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
    }

}
