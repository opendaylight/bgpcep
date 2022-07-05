/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.rib.impl.AbstractAddPathTest.AS_NUMBER;
import static org.opendaylight.protocol.bgp.rib.impl.AbstractAddPathTest.BGP_ID;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.dom.adapter.CurrentAdapterSerializer;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.mock.BGPMock;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.util.HexDumpBGPFileParser;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Ignore
public class ParserToSalTest extends DefaultRibPoliciesMockTest {

    private static final String TEST_RIB_ID = "testRib";
    private static final TablesKey TABLE_KEY
            = new TablesKey(LinkstateAddressFamily.VALUE, LinkstateSubsequentAddressFamily.VALUE);
    private static final InstanceIdentifier<BgpRib> BGP_IID = InstanceIdentifier.create(BgpRib.class);
    private final IpAddressNoZone localAddress = new IpAddressNoZone(new Ipv4AddressNoZone("127.0.0.1"));
    private BGPMock mock;
    private final RIBExtensionProviderActivator baseact = new RIBActivator();
    private final RIBExtensionProviderActivator lsact = new org.opendaylight.protocol.bgp.linkstate.impl.RIBActivator();
    private final RIBExtensionProviderContext ext1 = new SimpleRIBExtensionProviderContext();
    private final RIBExtensionProviderContext ext2 = new SimpleRIBExtensionProviderContext();
    @Mock
    private BGPDispatcher dispatcher;
    private ConstantCodecsRegistry codecsRegistry;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final String hexMessages = "/bgp_hex.txt";
        final List<byte[]> bgpMessages = HexDumpBGPFileParser
                .parseMessages(ParserToSalTest.class.getResourceAsStream(hexMessages));
        mock = new BGPMock(new EventBus("test"),
            ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst().orElseThrow().getMessageRegistry(),
            Lists.newArrayList(fixMessages(bgpMessages)));

        doReturn(GlobalEventExecutor.INSTANCE.newSucceededFuture(null)).when(dispatcher)
                .createReconnectingClient(any(InetSocketAddress.class), any(InetSocketAddress.class),
                        anyInt(), any(KeyMapping.class));

        final CurrentAdapterSerializer serializer = mappingService.currentSerializer();
        baseact.startRIBExtensionProvider(ext1, serializer);
        lsact.startRIBExtensionProvider(ext2, serializer);
        codecsRegistry = new ConstantCodecsRegistry(serializer);
    }

    @Test
    public void testWithLinkstate() throws InterruptedException, ExecutionException {
        final List<BgpTableType> tables = List.of(new BgpTableTypeImpl(LinkstateAddressFamily.VALUE,
                LinkstateSubsequentAddressFamily.VALUE));

        final RIBImpl rib = new RIBImpl(tableRegistry, new RibId(TEST_RIB_ID), AS_NUMBER, BGP_ID, ext2,
                dispatcher, codecsRegistry, getDomBroker(), policies,
                tables, Map.of(TABLE_KEY, BasePathSelectionModeFactory.createBestPathSelectionStrategy()));
        rib.instantiateServiceInstance();
        assertTablesExists(tables);
        final BGPPeer peer = AbstractAddPathTest.configurePeer(tableRegistry,
            localAddress.getIpv4AddressNoZone(), rib, null, PeerRole.Ibgp, new StrictBGPPeerRegistry());
        peer.instantiateServiceInstance();
        final ListenerRegistration<?> reg = mock.registerUpdateListener(peer);
        reg.close();
    }

    @Test
    public void testWithoutLinkstate() throws InterruptedException, ExecutionException {
        final List<BgpTableType> tables = List.of(new BgpTableTypeImpl(Ipv4AddressFamily.VALUE,
                UnicastSubsequentAddressFamily.VALUE));
        final RIBImpl rib = new RIBImpl(tableRegistry, new RibId(TEST_RIB_ID), AS_NUMBER, BGP_ID, ext1,
                dispatcher, codecsRegistry, getDomBroker(), policies,
                tables, Map.of(TABLE_KEY, BasePathSelectionModeFactory.createBestPathSelectionStrategy()));
        rib.instantiateServiceInstance();
        assertTablesExists(tables);
        final BGPPeer peer = AbstractAddPathTest.configurePeer(tableRegistry,
            localAddress.getIpv4AddressNoZone(), rib, null, PeerRole.Ibgp, new StrictBGPPeerRegistry());
        peer.instantiateServiceInstance();
        final ListenerRegistration<?> reg = mock.registerUpdateListener(peer);
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
            final var tables = bgpRib.nonnullRib().values().iterator().next().getLocRib().getTables();
            assertNotNull(tables);

            for (final BgpTableType tableType : expectedTables) {
                boolean found = false;
                for (final Tables table : tables.values()) {
                    if (table.getAfi().equals(tableType.getAfi()) && table.getSafi().equals(tableType.getSafi())) {
                        found = true;
                        assertEquals(Boolean.TRUE, table.getAttributes().getUptodate());
                    }
                }
                assertTrue(found);
            }
            return bgpRib;
        });
    }
}
