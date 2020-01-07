/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Ignore
public class ParserToSalTest extends DefaultRibPoliciesMockTest {

    private static final String TEST_RIB_ID = "testRib";
    private static final TablesKey TABLE_KEY
            = new TablesKey(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);
    private static final InstanceIdentifier<BgpRib> BGP_IID = InstanceIdentifier.create(BgpRib.class);
    private final IpAddressNoZone localAddress = new IpAddressNoZone(new Ipv4AddressNoZone("127.0.0.1"));
    private BGPMock mock;
    private AbstractRIBExtensionProviderActivator baseact;
    private AbstractRIBExtensionProviderActivator lsact;
    private RIBExtensionProviderContext ext1;
    private RIBExtensionProviderContext ext2;
    @Mock
    private BGPDispatcher dispatcher;
    private CodecsRegistryImpl codecsRegistry;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final String hexMessages = "/bgp_hex.txt";
        final List<byte[]> bgpMessages = HexDumpBGPFileParser
                .parseMessages(ParserToSalTest.class.getResourceAsStream(hexMessages));
        this.mock = new BGPMock(new EventBus("test"), ServiceLoaderBGPExtensionProviderContext
                .getSingletonInstance().getMessageRegistry(), Lists.newArrayList(fixMessages(bgpMessages)));

        Mockito.doReturn(GlobalEventExecutor.INSTANCE.newSucceededFuture(null)).when(this.dispatcher)
                .createReconnectingClient(any(InetSocketAddress.class), any(InetSocketAddress.class),
                        anyInt(), any(KeyMapping.class));

        this.ext1 = new SimpleRIBExtensionProviderContext();
        this.ext2 = new SimpleRIBExtensionProviderContext();
        this.baseact = new RIBActivator();
        this.lsact = new org.opendaylight.protocol.bgp.linkstate.impl.RIBActivator();

        this.baseact.startRIBExtensionProvider(this.ext1, this.mappingService);
        this.lsact.startRIBExtensionProvider(this.ext2, this.mappingService);
        this.codecsRegistry = CodecsRegistryImpl.create(this.bindingCodecTreeFactory,
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
    }

    @Override
    @After
    public void tearDown() {
        this.lsact.close();
        this.baseact.close();
    }

    @Test
    public void testWithLinkstate() throws InterruptedException, ExecutionException {
        final List<BgpTableType> tables = ImmutableList.of(new BgpTableTypeImpl(LinkstateAddressFamily.class,
                LinkstateSubsequentAddressFamily.class));

        final RIBImpl rib = new RIBImpl(this.tableRegistry, new RibId(TEST_RIB_ID), AS_NUMBER, BGP_ID, this.ext2,
                this.dispatcher, this.codecsRegistry, getDomBroker(), getDataBroker(), this.policies,
                tables, Collections.singletonMap(TABLE_KEY, BasePathSelectionModeFactory
                .createBestPathSelectionStrategy()));
        rib.instantiateServiceInstance();
        assertTablesExists(tables);
        rib.onGlobalContextUpdated(this.schemaService.getGlobalContext());
        final BGPPeer peer = AbstractAddPathTest.configurePeer(this.tableRegistry,
            this.localAddress.getIpv4AddressNoZone(), rib, null, PeerRole.Ibgp, new StrictBGPPeerRegistry());
        peer.instantiateServiceInstance();
        final ListenerRegistration<?> reg = this.mock.registerUpdateListener(peer);
        reg.close();
    }

    @Test
    public void testWithoutLinkstate() throws InterruptedException, ExecutionException {
        final List<BgpTableType> tables = ImmutableList.of(new BgpTableTypeImpl(Ipv4AddressFamily.class,
                UnicastSubsequentAddressFamily.class));
        final RIBImpl rib = new RIBImpl(this.tableRegistry, new RibId(TEST_RIB_ID), AS_NUMBER, BGP_ID, this.ext1,
                this.dispatcher, this.codecsRegistry, getDomBroker(), getDataBroker(), this.policies,
                tables, Collections.singletonMap(TABLE_KEY,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy()));
        rib.instantiateServiceInstance();
        rib.onGlobalContextUpdated(this.schemaService.getGlobalContext());
        assertTablesExists(tables);
        final BGPPeer peer = AbstractAddPathTest.configurePeer(this.tableRegistry,
            this.localAddress.getIpv4AddressNoZone(), rib, null, PeerRole.Ibgp, new StrictBGPPeerRegistry());
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

    private void assertTablesExists(final List<BgpTableType> expectedTables) throws InterruptedException,
            ExecutionException {
        readDataOperational(getDataBroker(), BGP_IID, bgpRib -> {
            final List<Tables> tables = bgpRib.getRib().get(0).getLocRib().getTables();
            assertFalse(tables.isEmpty());

            for (final BgpTableType tableType : expectedTables) {
                boolean found = false;
                for (final Tables table : tables) {
                    if (table.getAfi().equals(tableType.getAfi()) && table.getSafi().equals(tableType.getSafi())) {
                        found = true;
                        assertEquals(Boolean.TRUE, table.getAttributes().isUptodate());
                    }
                }
                assertTrue(found);
            }
            return bgpRib;
        });
    }
}
