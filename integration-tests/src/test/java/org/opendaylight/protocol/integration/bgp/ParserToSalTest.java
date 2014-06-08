/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration.bgp;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.RIBActivator;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.mock.BGPMock;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.util.HexDumpBGPFileParser;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserToSalTest {

    private static final Logger LOG = LoggerFactory.getLogger(ParserToSalTest.class);
    private static final InstanceIdentifier<Attributes> attrId = InstanceIdentifier.builder(BgpRib.class).child(Rib.class).child(
            LocRib.class).child(Tables.class).child(Attributes.class).build();

    private final String hex_messages = "/bgp_hex.txt";

    private BGPMock mock;
    private AbstractRIBExtensionProviderActivator baseact, lsact;
    private RIBExtensionProviderContext ext;

    @Mock
    DataModificationTransaction mockedTransaction;

    @Mock
    DataProviderService providerService;

    @Mock
    BGPDispatcher dispatcher;

    @Mock
    ReconnectStrategyFactory tcpStrategyFactory;

    @Mock
    ReconnectStrategyFactory sessionStrategy;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final List<byte[]> bgpMessages = HexDumpBGPFileParser.parseMessages(ParserToSalTest.class.getResourceAsStream(this.hex_messages));
        this.mock = new BGPMock(new EventBus("test"), ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry(), Lists.newArrayList(fixMessages(bgpMessages)));

        Mockito.doReturn(this.mockedTransaction).when(this.providerService).beginTransaction();

        Mockito.doReturn(new Future<RpcResult<TransactionStatus>>() {
            int i = 0;

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                LOG.debug("Cancel.");
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                this.i++;
                LOG.debug("Done. {}", this.i);
                return true;
            }

            @Override
            public RpcResult<TransactionStatus> get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public RpcResult<TransactionStatus> get(final long timeout, final TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
                return null;
            }
        }).when(this.mockedTransaction).commit();

        final HashMap<Object, Object> data = new HashMap<>();

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                LOG.debug("Put value {}", args[1]);
                data.put(args[0], args[1]);
                return null;
            }

        }).when(this.mockedTransaction).putOperationalData(Matchers.any(InstanceIdentifier.class), Matchers.any(DataObject.class));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                final InstanceIdentifier<?> id = (InstanceIdentifier<?>) args[0];

                LOG.debug("Remove key {}", id);
                data.remove(id);
                return null;
            }
        }).when(this.mockedTransaction).removeOperationalData(Matchers.any(InstanceIdentifier.class));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                final InstanceIdentifier<?> id = (InstanceIdentifier<?>) args[0];

                LOG.debug("Get key {}", id);
                Object ret = data.get(id);
                if (ret != null) {
                    return ret;
                }

                if (attrId.containsWildcarded(id)) {
                    return new AttributesBuilder().setUptodate(true).build();
                }
                return null;
            }

        }).when(this.mockedTransaction).readOperationalData(Matchers.any(InstanceIdentifier.class));

        Mockito.doReturn(GlobalEventExecutor.INSTANCE.newSucceededFuture(null)).when(this.dispatcher).createReconnectingClient(
                Mockito.any(InetSocketAddress.class), Mockito.any(BGPSessionPreferences.class), Mockito.any(AsNumber.class),
                Mockito.any(BGPSessionListener.class), Mockito.eq(this.tcpStrategyFactory), Mockito.eq(this.sessionStrategy),
                Mockito.any(KeyMapping.class));

        this.ext = new SimpleRIBExtensionProviderContext();
        this.baseact = new RIBActivator();
        this.lsact = new org.opendaylight.protocol.bgp.linkstate.RIBActivator();

        this.baseact.startRIBExtensionProvider(this.ext);
        this.lsact.startRIBExtensionProvider(this.ext);
    }

    @After
    public void tearDown() {
        this.lsact.close();
        this.baseact.close();
    }

    private void runTestWithTables(final List<BgpTableType> tables) {
        final RIBImpl rib = new RIBImpl(new RibId("testRib"), new AsNumber(72L), new Ipv4Address("127.0.0.1"), this.ext, this.dispatcher, this.tcpStrategyFactory, this.sessionStrategy, this.providerService, tables);
        final BGPPeer peer = new BGPPeer("peer-" + this.mock.toString(), null, null, null, rib.getLocalAs(), rib);

        ListenerRegistration<?> reg = this.mock.registerUpdateListener(peer);
        reg.close();

        Mockito.verify(this.mockedTransaction, Mockito.times(31)).commit();
    }

    @Test
    public void testWithLinkstate() {
        runTestWithTables(ImmutableList.of(
                (BgpTableType) new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class),
                new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class)));

        Mockito.verify(this.mockedTransaction, Mockito.times(83)).putOperationalData(Matchers.any(InstanceIdentifier.class),
                Matchers.any(DataObject.class));
    }

    @Test
    public void testWithoutLinkstate() {
        runTestWithTables(ImmutableList.of((BgpTableType) new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)));

        Mockito.verify(this.mockedTransaction, Mockito.times(28)).putOperationalData(Matchers.any(InstanceIdentifier.class),
                Matchers.any(DataObject.class));
    }

    private Collection<byte[]> fixMessages(final Collection<byte[]> bgpMessages) {
        return Collections2.transform(bgpMessages, new Function<byte[], byte[]>() {

            @Nullable
            @Override
            public byte[] apply(@Nullable final byte[] input) {
                final byte[] ret = new byte[input.length + 1];
                // ff
                ret[0] = -1;
                for (int i = 0; i < input.length; i++) {
                    ret[i + 1] = input[i];
                }
                return ret;
            }
        });
    }
}
