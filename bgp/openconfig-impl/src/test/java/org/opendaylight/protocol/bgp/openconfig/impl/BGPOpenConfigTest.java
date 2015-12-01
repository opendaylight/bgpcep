/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.MountPointService.MountPointListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPAppPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPRibInstanceConfiguration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BGPOpenConfigTest {

    private final BGPOpenConfig bgpOpenConfig = new BGPOpenConfig();

    private final ConsumerContext session = Mockito.mock(ConsumerContext.class);

    private final DataBroker dataBroker = Mockito.mock(DataBroker.class);
    private final MountPointService mountService = Mockito.mock(MountPointService.class);
    private final ListenerRegistration<?> registration = Mockito.mock(ListenerRegistration.class);

    private final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
    private final WriteTransaction writeTx = Mockito.mock(WriteTransaction.class);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(this.dataBroker).when(this.session).getSALService(DataBroker.class);
        Mockito.doReturn(this.mountService).when(this.session).getSALService(MountPointService.class);
        Mockito.doReturn(this.registration).when(this.dataBroker).registerDataTreeChangeListener(Mockito.any(DataTreeIdentifier.class), Mockito.any(DataTreeChangeListener.class));
        Mockito.doReturn(this.txChain).when(this.dataBroker).createTransactionChain(Mockito.any(TransactionChainListener.class));
        Mockito.doReturn(this.writeTx).when(this.txChain).newWriteOnlyTransaction();
        Mockito.doNothing().when(this.txChain).close();
        Mockito.doNothing().when(this.writeTx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class));
        Mockito.doNothing().when(this.writeTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        final CheckedFuture<?, ?> checkedFuture = Mockito.mock(CheckedFuture.class);
        Mockito.doReturn(checkedFuture).when(this.writeTx).submit();
        Mockito.doReturn(null).when(checkedFuture).checkedGet();

        final MountPoint mp = Mockito.mock(MountPoint.class);
        Mockito.doReturn(Optional.of(this.dataBroker)).when(mp).getService(DataBroker.class);
        final ListenerRegistration<MountPointListener> mpRegistration = Mockito.mock(ListenerRegistration.class);
        Mockito.doNothing().when(mpRegistration).close();
        Mockito.doReturn(mpRegistration).when(this.mountService).registerListener(Mockito.<InstanceIdentifier<?>>any(), Mockito.any(MountPointListener.class));
        Mockito.doReturn(Optional.of(mp)).when(this.mountService).getMountPoint(Mockito.any(InstanceIdentifier.class));

        this.bgpOpenConfig.onSessionInitialized(this.session);
    }

    @After
    public void tearDown() {
        this.bgpOpenConfig.close();
    }

    @Test
    public void testOnSessionInitialized() {
        Mockito.verify(this.session).getSALService(DataBroker.class);
        Mockito.verify(this.session).getSALService(MountPointService.class);
    }

    @Test
    public void testOnMountPoint() {
        this.bgpOpenConfig.onMountPointCreated(null);
        Mockito.verify(this.mountService).getMountPoint(Mockito.any(InstanceIdentifier.class));
        Mockito.verify(this.dataBroker).registerDataTreeChangeListener(Mockito.any(DataTreeIdentifier.class), Mockito.any(DataTreeChangeListener.class));
        this.bgpOpenConfig.onMountPointRemoved(null);
        Mockito.verify(this.registration).close();
    }

    @Test
    public void testGetOpenConfigMapper() {
        final BGPOpenconfigMapper<BGPRibInstanceConfiguration> rib = this.bgpOpenConfig.getOpenConfigMapper(BGPRibInstanceConfiguration.class);
        Assert.assertNotNull(rib);
        final BGPOpenconfigMapper<BGPPeerInstanceConfiguration> peer = this.bgpOpenConfig.getOpenConfigMapper(BGPPeerInstanceConfiguration.class);
        Assert.assertNotNull(peer);
        final BGPOpenconfigMapper<BGPAppPeerInstanceConfiguration> appPeer = this.bgpOpenConfig.getOpenConfigMapper(BGPAppPeerInstanceConfiguration.class);
        Assert.assertNotNull(appPeer);
    }

}
