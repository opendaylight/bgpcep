/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.RD_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValueBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EvpnNlriParserTest.createMACIpAdvChoice;
import static org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEvpnNlriRegistryTest.EVPN_NID;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest;
import org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriActivator;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.destination.EvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListNodeBuilder;

public class EvpnRibSupportTest {

    private static final NodeIdentifier RD_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME, "route-distinguisher").intern());
    private static final NodeIdentifier ROUTES_NODE_ID = new NodeIdentifier(Routes.QNAME);
    private static final Ipv4Address ipv4 = new Ipv4Address("42.42.42.42");
    private static final NodeIdentifier DESTINATION_NID = NodeIdentifier.create(EvpnDestination.QNAME);
    private final EvpnRibSupport evpnRibSupport = EvpnRibSupport.getInstance();
    private final List<MapEntryNode> evpnList = new ArrayList<>();
    private List<YangInstanceIdentifier> routes;
    @Mock
    private DOMDataWriteTransaction tx;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        routes = new ArrayList<>();
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                EvpnRibSupportTest.this.routes.add((YangInstanceIdentifier) args[1]);
                return args[1];
            }
        }).when(this.tx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                EvpnRibSupportTest.this.routes.remove(args[1]);
                return args[1];
            }
        }).when(this.tx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class));
    }


    @Test
    public void testbuildReach() throws BGPParsingException {
        final CNextHop hop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(ipv4).build()).build();
        final MpReachNlri result = evpnRibSupport.buildReach(evpnList, hop);
        assertEquals(L2vpnAddressFamily.class, result.getAfi());
        assertEquals(EvpnSubsequentAddressFamily.class, result.getSafi());
        assertEquals(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("42.42.42.42")).build()).build(), result.getCNextHop());
    }

    @Test
    public void testBuildUnreach() {
        final MpUnreachNlri result = evpnRibSupport.buildUnreach(evpnList);
        assertEquals(L2vpnAddressFamily.class, result.getAfi());
        assertEquals(EvpnSubsequentAddressFamily.class, result.getSafi());
    }

    @Test
    public void testDestRoutesEthADRModel() {
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
        NlriActivator.registerNlriParsers(new ArrayList<>());

        final DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> evpnBI = ImmutableUnkeyedListEntryNodeBuilder.create();
        evpnBI.withNodeIdentifier(EVPN_NID);
        evpnBI.withChild(EthADRParserTest.createEthADRModel());
        evpnBI.withChild(createValueBuilder(RD_MODEL, RD_NID).build());

        final UnkeyedListNode routes = ImmutableUnkeyedListNodeBuilder.create().withNodeIdentifier(DESTINATION_NID).addChild(evpnBI.build()).build();
        final ContainerNode destination = ImmutableContainerNodeBuilder.create().addChild(routes).withNodeIdentifier(DESTINATION_NID).build();
        final ContainerNode attributes = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(Attributes.QNAME)).build();

        final YangInstanceIdentifier yangIdentifier = YangInstanceIdentifier.of(Routes.QNAME);
        evpnRibSupport.putDestinationRoutes(tx, yangIdentifier, destination, attributes, ROUTES_NODE_ID);
        Assert.assertEquals(1, this.routes.size());

        evpnRibSupport.deleteDestinationRoutes(tx, yangIdentifier, destination, ROUTES_NODE_ID);
        Assert.assertEquals(0, this.routes.size());
    }

    @Test
    public void testDestRoutesMacIp() {
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
        NlriActivator.registerNlriParsers(new ArrayList<>());

        final DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> evpnBI = ImmutableUnkeyedListEntryNodeBuilder.create();
        evpnBI.withNodeIdentifier(EVPN_NID);
        evpnBI.withChild(createMACIpAdvChoice());
        evpnBI.withChild(createValueBuilder(RD_MODEL, RD_NID).build());

        final UnkeyedListNode routes = ImmutableUnkeyedListNodeBuilder.create().withNodeIdentifier(DESTINATION_NID).addChild(evpnBI.build()).build();
        final ContainerNode destination = ImmutableContainerNodeBuilder.create().addChild(routes).withNodeIdentifier(DESTINATION_NID).build();
        final ContainerNode attributes = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(Attributes.QNAME)).build();

        final YangInstanceIdentifier yangIdentifier = YangInstanceIdentifier.of(Routes.QNAME);
        evpnRibSupport.putDestinationRoutes(tx, yangIdentifier, destination, attributes, ROUTES_NODE_ID);
        Assert.assertEquals(1, this.routes.size());

        evpnRibSupport.deleteDestinationRoutes(tx, yangIdentifier, destination, ROUTES_NODE_ID);
        Assert.assertEquals(0, this.routes.size());
    }
}