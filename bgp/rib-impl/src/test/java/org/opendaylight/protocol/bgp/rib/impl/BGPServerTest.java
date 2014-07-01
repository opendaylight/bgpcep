/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.server.BGPServerSessionValidator;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.as4.bytes._case.As4BytesCapabilityBuilder;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;

public class BGPServerTest {

    @Mock
    DataModificationTransaction mockedTransaction;

    @Mock
    DataProviderService providerService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateServer() throws Exception {
        BGPDispatcherImpl dispatcher = new BGPDispatcherImpl(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry(), new HashedWheelTimer(), new NioEventLoopGroup(), new NioEventLoopGroup());

        InetSocketAddress adr = new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), 8383);
        BGPSessionPreferences prefs = new BGPSessionPreferences(new AsNumber(1L), 50, new Ipv4Address("0.0.0.0"), Lists.newArrayList(new BgpParametersBuilder().setCParameters(new As4BytesCaseBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(1L)).build()).build()).build()));

        List<BgpTableType> tables = ImmutableList.of(
                (BgpTableType) new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class),
                new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

        ReconnectStrategyFactory tcpStrategyFactory = new ReconnectStrategyFactory() {
            @Override
            public ReconnectStrategy createReconnectStrategy() {
                return new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000);
            }
        };
        final RIBImpl rib = new RIBImpl(new RibId("testRib"), new AsNumber(72L), new Ipv4Address("0.0.0.0"), new SimpleRIBExtensionProviderContext(), dispatcher, tcpStrategyFactory, tcpStrategyFactory, getProviderService(), tables);

        dispatcher.createServer(adr, prefs, rib, new BGPServerSessionValidator());

        Thread.sleep(1000000);
    }

    private static final InstanceIdentifier<Attributes> attrId = InstanceIdentifier.builder(BgpRib.class).child(Rib.class).child(
            LocRib.class).child(Tables.class).child(Attributes.class).build();

    public DataProviderService getProviderService() {
        Mockito.doReturn(this.mockedTransaction).when(this.providerService).beginTransaction();

        Mockito.doReturn(new Future<RpcResult<TransactionStatus>>() {
            int i = 0;

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                this.i++;
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
                data.put(args[0], args[1]);
                return null;
            }

        }).when(this.mockedTransaction).putOperationalData(Matchers.any(InstanceIdentifier.class), Matchers.any(DataObject.class));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                final InstanceIdentifier<?> id = (InstanceIdentifier<?>) args[0];

                data.remove(id);
                return null;
            }
        }).when(this.mockedTransaction).removeOperationalData(Matchers.any(InstanceIdentifier.class));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                final InstanceIdentifier<?> id = (InstanceIdentifier<?>) args[0];

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

        return providerService;
    }
}
