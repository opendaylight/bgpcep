/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentOperational;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentOperational;
import static org.opendaylight.protocol.util.CheckUtil.checkReceivedMessages;
import static org.opendaylight.protocol.util.CheckUtil.readDataConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.base.VerifyException;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.util.CheckUtil.ListenerCheck;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CheckUtilTest extends AbstractConcurrentDataBrokerTest {
    private static final TopologyId TOPOLOGY_ID = new TopologyId("topotest");
    private static final KeyedInstanceIdentifier<Topology, TopologyKey> TOPOLOGY_IID =
            InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(TOPOLOGY_ID));
    private static final int TIMEOUT = 1;

    @Mock
    private ListenerCheck listenerCheck;
    @Mock
    private ChannelFuture future;

    @Test(expected = VerifyException.class)
    public void testWaitFutureSuccessFail() {
        doReturn(this.future).when(this.future).addListener(any());
        waitFutureSuccess(this.future, 10L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testWaitFutureSuccess() {
        when(this.future.isSuccess()).thenReturn(true);
        doAnswer(invocation -> {
            ((GenericFutureListener)invocation.getArgument(0)).operationComplete(CheckUtilTest.this.future);
            return CheckUtilTest.this.future;
        }).when(this.future).addListener(any());
        waitFutureSuccess(this.future);
    }

    @Test(expected = NullPointerException.class)
    public void testReadDataOperationalNull() throws Exception {
        readDataOperational(getDataBroker(), TOPOLOGY_IID, test -> false, TIMEOUT);
    }

    @Test(expected = NullPointerException.class)
    public void testReadDataConfigurationNull() throws Exception {
        readDataConfiguration(getDataBroker(), TOPOLOGY_IID, test -> false, TIMEOUT);
    }

    @Test(expected = AssertionError.class)
    public void testReadDataOperationalFail() throws Exception {
        storeTopo(LogicalDatastoreType.OPERATIONAL);
        readDataOperational(getDataBroker(), TOPOLOGY_IID, result -> {
            assertNotNull(result.getNode());
            return result;
        }, TIMEOUT);
    }

    @Test(expected = AssertionError.class)
    public void testReadDataConfigurationFail() throws Exception {
        storeTopo(LogicalDatastoreType.CONFIGURATION);
        readDataConfiguration(getDataBroker(), TOPOLOGY_IID, result -> {
            assertNotNull(result.getNode());
            return result;
        }, TIMEOUT);
    }

    @Test
    public void testReadDataOperational() throws Exception {
        storeTopo(LogicalDatastoreType.OPERATIONAL);
        readDataOperational(getDataBroker(), TOPOLOGY_IID, result -> {
            assertNull(result.getNode());
            return result;
        }, TIMEOUT);
    }

    @Test
    public void testReadDataConfiguration() throws Exception {
        storeTopo(LogicalDatastoreType.CONFIGURATION);
        readDataConfiguration(getDataBroker(), TOPOLOGY_IID, result -> {
            assertNull(result.getNode());
            return result;
        }, TIMEOUT);
    }

    private void storeTopo(final LogicalDatastoreType dsType) throws ExecutionException, InterruptedException {
        final WriteTransaction wt = getDataBroker().newWriteOnlyTransaction();
        wt.mergeParentStructurePut(dsType, TOPOLOGY_IID, new TopologyBuilder().setTopologyId(TOPOLOGY_ID).build());
        wt.commit().get();
    }

    @Test
    public void testCheckPresentConfiguration() throws Exception {
        storeTopo(LogicalDatastoreType.CONFIGURATION);
        checkPresentConfiguration(getDataBroker(), TOPOLOGY_IID);
    }

    @Test
    public void testCheckPresentOperational() throws Exception {
        storeTopo(LogicalDatastoreType.OPERATIONAL);
        checkPresentOperational(getDataBroker(), TOPOLOGY_IID);
    }

    @Test(expected = AssertionError.class)
    public void testCheckNotPresentOperationalFail() throws Exception {
        storeTopo(LogicalDatastoreType.OPERATIONAL);
        checkNotPresentOperational(getDataBroker(), TOPOLOGY_IID);
    }

    @Test
    public void testCheckNotPresentOperational() throws Exception {
        checkNotPresentOperational(getDataBroker(), TOPOLOGY_IID);
    }

    @Test
    public void testCheckNotPresentConfiguration() throws Exception {
        checkNotPresentConfiguration(getDataBroker(), TOPOLOGY_IID);
    }

    @Test(expected = AssertionError.class)
    public void testCheckEquals() throws Exception {
        checkEquals(Assert::fail, TIMEOUT);
    }

    @Test(expected = AssertionError.class)
    public void testCheckReceivedMessagesNotEqual() {
        doReturn(0).when(this.listenerCheck).getListMessageSize();
        checkReceivedMessages(this.listenerCheck, 1, TIMEOUT);
    }

    @Test
    public void testCheckReceivedMessagesEqual() {
        doReturn(1).when(this.listenerCheck).getListMessageSize();
        checkReceivedMessages(this.listenerCheck, 1, TIMEOUT);
    }
}
