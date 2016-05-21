/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

/**
 * TODO: Remove, instead use Common Rib Support test
 */
public class EvpnRibSupportTest {
/*
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
    }*/
}