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
import static org.opendaylight.protocol.bgp.rib.impl.AbstractAddPathTest.AS_NUMBER;
import static org.opendaylight.protocol.bgp.rib.impl.AbstractAddPathTest.BGP_ID;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.mock.BGPMock;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.util.HexDumpBGPFileParser;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

public class ParserToSalTest extends AbstractConcurrentDataBrokerTest {

    private static final String TEST_RIB_ID = "testRib";
    private static final TablesKey TABLE_KEY = new TablesKey(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);
    private static final InstanceIdentifier<BgpRib> BGP_IID = InstanceIdentifier.create(BgpRib.class);
    private BGPMock mock;
    private AbstractRIBExtensionProviderActivator baseact, lsact;
    private RIBExtensionProviderContext ext1, ext2;
    private final String localAddress = "127.0.0.1";

    @Mock
    private BGPDispatcher dispatcher;
    private BindingCodecTreeFactory codecFactory;

    private DOMSchemaService schemaService;

    @Before
    public void setUp() throws Exception {
        super.setup();
        MockitoAnnotations.initMocks(this);
        final String hexMessages = "/bgp_hex.txt";
        final List<byte[]> bgpMessages = HexDumpBGPFileParser.parseMessages(ParserToSalTest.class.getResourceAsStream(hexMessages));
        this.mock = new BGPMock(new EventBus("test"), ServiceLoaderBGPExtensionProviderContext
                .getSingletonInstance().getMessageRegistry(), Lists.newArrayList(fixMessages(bgpMessages)));

        Mockito.doReturn(GlobalEventExecutor.INSTANCE.newSucceededFuture(null)).when(this.dispatcher)
                .createReconnectingClient(Mockito.any(InetSocketAddress.class), Mockito.anyInt(),
                        Mockito.any(KeyMapping.class));

        this.ext1 = new SimpleRIBExtensionProviderContext();
        this.ext2 = new SimpleRIBExtensionProviderContext();
        this.baseact = new RIBActivator();
        this.lsact = new org.opendaylight.protocol.bgp.linkstate.impl.RIBActivator();

        this.baseact.startRIBExtensionProvider(this.ext1);
        this.lsact.startRIBExtensionProvider(this.ext2);
    }

    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableList.of(
                BindingReflections.getModuleInfo(Ipv4Route.class),
                BindingReflections.getModuleInfo(Ipv6Route.class),
                BindingReflections.getModuleInfo(LinkstateRoute.class));
    }

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        this.codecFactory = customizer.getBindingToNormalized();
        this.schemaService = customizer.getSchemaService();
        return customizer;
    }

    @After
    public void tearDown() {
        this.lsact.close();
        this.baseact.close();
    }

    @Test
    public void testWithLinkstate() throws InterruptedException, ExecutionException, ReadFailedException {
        final List<BgpTableType> tables = ImmutableList.of(new BgpTableTypeImpl(LinkstateAddressFamily.class,
                LinkstateSubsequentAddressFamily.class));
        final RIBImpl rib = new RIBImpl(new RibId(TEST_RIB_ID),
                AS_NUMBER, new BgpId("127.0.0.1"), null, this.ext2, this.dispatcher,
                this.codecFactory, getDomBroker(), tables, Collections.singletonMap(TABLE_KEY,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy()),
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
        rib.instantiateServiceInstance();
        assertTablesExists(tables);
        rib.onGlobalContextUpdated(this.schemaService.getGlobalContext());
        final BGPPeer peer = new BGPPeer(this.localAddress, rib, PeerRole.Ibgp, null, Collections.emptySet(),
                Collections.emptySet());
        peer.instantiateServiceInstance();
        final ListenerRegistration<?> reg = this.mock.registerUpdateListener(peer);
        reg.close();
    }

    @Test
    public void testWithoutLinkstate() throws InterruptedException, ExecutionException, ReadFailedException {
        final List<BgpTableType> tables = ImmutableList.of(new BgpTableTypeImpl(Ipv4AddressFamily.class,
                UnicastSubsequentAddressFamily.class));
        final RIBImpl rib = new RIBImpl(new RibId(TEST_RIB_ID), AS_NUMBER, BGP_ID,
                null, this.ext1, this.dispatcher, this.codecFactory, getDomBroker(), tables,
                Collections.singletonMap(TABLE_KEY, BasePathSelectionModeFactory.createBestPathSelectionStrategy()),
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
        rib.instantiateServiceInstance();
        rib.onGlobalContextUpdated(this.schemaService.getGlobalContext());
        assertTablesExists(tables);
        final BGPPeer peer = new BGPPeer(this.localAddress, rib, PeerRole.Ibgp, null, Collections.emptySet(),
                Collections.emptySet());
        peer.instantiateServiceInstance();
        final ListenerRegistration<?> reg = this.mock.registerUpdateListener(peer);
        reg.close();
    }

    private static Collection<byte[]> fixMessages(final Collection<byte[]> bgpMessages) {
        return Collections2.transform(bgpMessages, input -> {
            final byte[] ret = new byte[input.length + 1];
            // ff
            ret[0] = -1;
            System.arraycopy(input, 0, ret, 1, input.length);
            return ret;
        });
    }

    private void assertTablesExists(final List<BgpTableType> expectedTables)
            throws InterruptedException, ExecutionException, ReadFailedException {
        readDataOperational(getDataBroker(), BGP_IID, bgpRib -> {
            final List<Tables> tables = bgpRib.getRib().get(0).getLocRib().getTables();
            assertFalse(tables.isEmpty());

            for (final BgpTableType tableType : expectedTables) {
                boolean found = false;
                for (final Tables table : tables) {
                    if (table.getAfi().equals(tableType.getAfi()) && table.getSafi().equals(tableType.getSafi())) {
                        found = true;
                        assertTrue(Boolean.valueOf(true).equals(table.getAttributes().isUptodate()));
                    }
                }
                assertTrue(found);
            }
            return bgpRib;
        });
    }
}
