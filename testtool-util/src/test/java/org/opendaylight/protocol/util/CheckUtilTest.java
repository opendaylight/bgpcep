/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.util.CheckUtil.ListenerCheck;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class CheckUtilTest extends AbstractConcurrentDataBrokerTest {
    private static final TopologyId TOPOLOGY_ID = new TopologyId("topotest");
    private final KeyedInstanceIdentifier<Topology, TopologyKey> topologyIIdKeyed =
            InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                    new TopologyKey(TOPOLOGY_ID));
    private static int TIMEOUT = 1;
    @Mock
    private ListenerCheck listenerCheck;
    @Mock
    private ChannelFuture future;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = VerifyException.class)
    public void testWaitFutureSuccessFail() throws Exception {
        when(this.future.isDone()).thenReturn(false);
        doReturn(this.future).when(this.future).addListener(any());
        waitFutureSuccess(this.future);
    }

    @Test
    public void testWaitFutureSuccess() throws Exception {
        when(this.future.isSuccess()).thenReturn(true);
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, GenericFutureListener.class).operationComplete(CheckUtilTest.this.future);
            return CheckUtilTest.this.future;
        }).when(this.future).addListener(any());
        waitFutureSuccess(this.future);
    }

    @Test(expected = NullPointerException.class)
    public void testReadDataOperationalNull() throws Exception {
        readDataOperational(getDataBroker(), topologyIIdKeyed, test -> false, TIMEOUT);
    }

    @Test(expected = NullPointerException.class)
    public void testReadDataConfigurationNull() throws Exception {
        readDataConfiguration(getDataBroker(), topologyIIdKeyed, test -> false, TIMEOUT);
    }

    @Test(expected = AssertionError.class)
    public void testReadDataOperationalFail() throws Exception {
        storeTopo(LogicalDatastoreType.OPERATIONAL);
        readDataOperational(getDataBroker(), this.topologyIIdKeyed, result -> {
            assertNotNull(result.getNode());
            return result;
        }, TIMEOUT);
    }

    @Test(expected = AssertionError.class)
    public void testReadDataConfigurationFail() throws Exception {
        storeTopo(LogicalDatastoreType.CONFIGURATION);
        readDataConfiguration(getDataBroker(), this.topologyIIdKeyed, result -> {
            assertNotNull(result.getNode());
            return result;
        }, TIMEOUT);
    }

    @Test
    public void testReadDataOperational() throws Exception {
        storeTopo(LogicalDatastoreType.OPERATIONAL);
        readDataOperational(getDataBroker(), this.topologyIIdKeyed, result -> {
            assertNull(result.getNode());
            return result;
        }, TIMEOUT);
    }

    @Test
    public void testReadDataConfiguration() throws Exception {
        storeTopo(LogicalDatastoreType.CONFIGURATION);
        readDataConfiguration(getDataBroker(), this.topologyIIdKeyed, result -> {
            assertNull(result.getNode());
            return result;
        }, TIMEOUT);
    }

    private void storeTopo(final LogicalDatastoreType dsType) throws ExecutionException, InterruptedException {
        final WriteTransaction wt = getDataBroker().newWriteOnlyTransaction();
        wt.put(dsType, this.topologyIIdKeyed,
                new TopologyBuilder()
                        .setTopologyId(TOPOLOGY_ID)
                        .build(), true);
        wt.submit().get();
    }

    @Test
    public void testCheckPresentConfiguration() throws Exception {
        storeTopo(LogicalDatastoreType.CONFIGURATION);
        checkPresentConfiguration(getDataBroker(), this.topologyIIdKeyed);
    }
    @Test
    public void testCheckPresentOperational() throws Exception {
        storeTopo(LogicalDatastoreType.OPERATIONAL);
        checkPresentOperational(getDataBroker(), this.topologyIIdKeyed);
    }

    @Test(expected = AssertionError.class)
    public void testCheckNotPresentOperationalFail() throws Exception {
        storeTopo(LogicalDatastoreType.OPERATIONAL);
        checkNotPresentOperational(getDataBroker(), this.topologyIIdKeyed);
    }

    @Test
    public void testCheckNotPresentOperational() throws Exception {
        checkNotPresentOperational(getDataBroker(), this.topologyIIdKeyed);
    }

    @Test
    public void testCheckNotPresentConfiguration() throws Exception {
        checkNotPresentConfiguration(getDataBroker(), this.topologyIIdKeyed);
    }

    @Test(expected = AssertionError.class)
    public void testCheckEquals() throws Exception {
        checkEquals(() -> assertTrue(false), TIMEOUT);
    }

    @Test(expected = AssertionError.class)
    public void testCheckReceivedMessagesNotEqual() throws Exception {
        doReturn(0).when(this.listenerCheck).getListMessageSize();
        checkReceivedMessages(this.listenerCheck, 1, TIMEOUT);
    }

    @Test
    public void testCheckReceivedMessagesEqual() throws Exception {
        doReturn(1).when(this.listenerCheck).getListMessageSize();
        checkReceivedMessages(this.listenerCheck, 1, TIMEOUT);
    }
}