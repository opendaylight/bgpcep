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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPRibInstanceConfiguration;
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

    @Test
    public void testOnDataTreeChanged() {
        // TODO problem with mocking final classes and methods
    }

    @After
    public void close() {
        CONFIG.close();
    }
}