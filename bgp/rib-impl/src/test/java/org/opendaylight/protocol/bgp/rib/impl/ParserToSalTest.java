/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.mock.BGPMock;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.util.HexDumpBGPFileParser;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

public class ParserToSalTest extends AbstractDataBrokerTest {

    private static final String TEST_RIB_ID = "testRib";

    private final String hex_messages = "/bgp_hex.txt";

    private BGPMock mock;
    private AbstractRIBExtensionProviderActivator baseact, lsact;
    private RIBExtensionProviderContext ext1, ext2;

    @Mock
    BGPDispatcher dispatcher;

    @Mock
    ReconnectStrategyFactory tcpStrategyFactory;

    @Mock
    ReconnectStrategyFactory sessionStrategy;

    BindingCodecTreeFactory codecFactory;

    private SchemaService schemaService;

    @Override
    protected java.lang.Iterable<org.opendaylight.yangtools.yang.binding.YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableList.of(BindingReflections.getModuleInfo(Ipv4Route.class), BindingReflections.getModuleInfo(Ipv6Route.class), BindingReflections.getModuleInfo(LinkstateRoute.class));
    };

    @Override
    protected DataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final DataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        this.codecFactory = customizer.getBindingToNormalized();
        this.schemaService = customizer.getSchemaService();
        return customizer;
    }

    @Override
    protected void setupWithDataBroker(final DataBroker dataBroker) {
        MockitoAnnotations.initMocks(this);
        final List<byte[]> bgpMessages;
        try {
            bgpMessages = HexDumpBGPFileParser.parseMessages(ParserToSalTest.class.getResourceAsStream(this.hex_messages));
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
        this.mock = new BGPMock(new EventBus("test"), ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry(), Lists.newArrayList(fixMessages(bgpMessages)));

        Mockito.doReturn(GlobalEventExecutor.INSTANCE.newSucceededFuture(null)).when(this.dispatcher).createReconnectingClient(
                Mockito.any(InetSocketAddress.class), Mockito.any(BGPPeerRegistry.class), Mockito.eq(this.tcpStrategyFactory), Mockito.any(Optional.class));

        this.ext1 = new SimpleRIBExtensionProviderContext();
        this.ext2 = new SimpleRIBExtensionProviderContext();
        this.baseact = new RIBActivator();
        this.lsact = new org.opendaylight.protocol.bgp.linkstate.RIBActivator();

        this.baseact.startRIBExtensionProvider(this.ext1);
        this.lsact.startRIBExtensionProvider(this.ext2);
    }

    @After
    public void tearDown() {
        this.lsact.close();
        this.baseact.close();
    }

    @Test
    public void testWithLinkstate() throws InterruptedException, ExecutionException {
        final List<BgpTableType> tables = ImmutableList.of(
                (BgpTableType) new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));
        final RIBImpl rib = new RIBImpl(new RibId(TEST_RIB_ID), new AsNumber(72L), new Ipv4Address("127.0.0.1"), null, this.ext2, this.dispatcher, this.tcpStrategyFactory, this.codecFactory, this.sessionStrategy, getDataBroker(), getDomBroker(), tables, GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
        assertTablesExists(tables, true);
        rib.onGlobalContextUpdated(this.schemaService.getGlobalContext());
        final BGPPeer peer = new BGPPeer("peer-" + this.mock.toString(), rib);

        final ListenerRegistration<?> reg = this.mock.registerUpdateListener(peer);
        reg.close();
    }

    @Test
    public void testWithoutLinkstate() throws InterruptedException, ExecutionException {
        final List<BgpTableType> tables = ImmutableList.of((BgpTableType) new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        final RIBImpl rib = new RIBImpl(new RibId(TEST_RIB_ID), new AsNumber(72L), new Ipv4Address("127.0.0.1"), null, this.ext1, this.dispatcher, this.tcpStrategyFactory, this.codecFactory, this.sessionStrategy, getDataBroker(), getDomBroker(), tables, GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
        rib.onGlobalContextUpdated(this.schemaService.getGlobalContext());
        assertTablesExists(tables, true);
        final BGPPeer peer = new BGPPeer("peer-" + this.mock.toString(), rib);

        final ListenerRegistration<?> reg = this.mock.registerUpdateListener(peer);
        reg.close();
    }

    private static Collection<byte[]> fixMessages(final Collection<byte[]> bgpMessages) {
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

    private void assertTablesExists(final List<BgpTableType> expectedTables, final boolean uptodate) throws InterruptedException, ExecutionException {
        final Optional<LocRib> lockRib = getLocRibTable();
        assertTrue(lockRib.isPresent());
        final List<Tables> tables = lockRib.get().getTables();
        assertFalse(tables.isEmpty());
        for (final BgpTableType tableType : expectedTables) {
            boolean found = false;
            for (final Tables table : tables) {
                if(table.getAfi().equals(tableType.getAfi()) && table.getSafi().equals(tableType.getSafi())) {
                    found = true;
                    assertTrue(Boolean.valueOf(uptodate).equals(table.getAttributes().isUptodate()));
                }
            }
            assertTrue(found);
        }
    }

    private Optional<LocRib> getLocRibTable() throws InterruptedException, ExecutionException {
        return getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(new RibId(TEST_RIB_ID))).child(LocRib.class).build()).get();
    }
}
