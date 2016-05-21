/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

/**
 * TODO: Remove, instead use Common Rib Support test
 */
public class AbstractRIBSupportTest {
   /* private final ContainerNode ipv4p = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(Ipv4Prefixes.QNAME)).build();
    private final ContainerNode destination = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(DestinationIpv4.QNAME)).addChild(this.ipv4p).build();
    private final ChoiceNode choiceNode = ImmutableChoiceNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(DestinationType.QNAME)).addChild(this.destination).build();
    static ContainerNode dest;

    private final RIBSupport testSupport = new AbstractRIBSupport(Ipv4RoutesCase.class, Ipv4Routes.class, Ipv4Route.class,
        Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, DestinationIpv4.QNAME) {
        @Override
        public ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
            return null;
        }

        @Override
        public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
            return null;
        }

        @Nonnull
        @Override
        protected DestinationType buildDestination(@Nonnull final Collection<MapEntryNode> routes) {
            return null;
        }

        @Nonnull
        @Override
        protected DestinationType buildWithdrawnDestination(@Nonnull final Collection<MapEntryNode> routes) {
            return null;
        }

        @Override
        protected void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath, final ContainerNode destination, final ContainerNode attributes, final ApplyRoute applyFunction) {

        }

        @Override
        public boolean isComplexRoute() {
            return false;
        }
    };

    @Mock
    private DOMDataWriteTransaction tx;

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(QName.create(Ipv4Routes.QNAME, Attributes.QNAME.getLocalName())), this.testSupport.routeAttributesIdentifier());
    }

    @Test
    public void testChangedRoutes() {
        final QName TEST_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet:test", "2015-03-05", "test");
        final YangInstanceIdentifier writePath = YangInstanceIdentifier.of(TEST_QNAME);

        final YangInstanceIdentifier.NodeWithValue nodeIdentifier = new YangInstanceIdentifier.NodeWithValue(Ipv4Route.QNAME, "route");
        final LeafSetEntryNode<Object> routeEntry = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(nodeIdentifier).withValue("route").build();
        final LeafSetNode<Object> route = ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(Ipv4Route.QNAME)).withChild(routeEntry).build();
        ContainerNode routes = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(Ipv4Routes.QNAME))
            .withChild(route).build();

        final ContainerNode routesContainer = ImmutableContainerNodeBuilder.create().addChild(routes).
            withNodeIdentifier(new NodeIdentifier(TEST_QNAME)).build();
        final DataTreeCandidate candidate = DataTreeCandidates.fromNormalizedNode(writePath, routesContainer);
        final Collection<DataTreeCandidateNode> output = this.testSupport.changedRoutes(candidate.getRootNode());

        Assert.assertFalse(output.isEmpty());
        assertEquals(nodeIdentifier.toString(), output.iterator().next().getIdentifier().toString());
    }


    @Test
    public void testRoutePath() {
        final YangInstanceIdentifier routePath = YangInstanceIdentifier.of(Routes.QNAME);
        final NodeIdentifier routeId = new NodeIdentifier(Ipv4Route.QNAME);
        final String result = "/(urn:opendaylight:params:xml:ns:yang:bgp-rib?revision=2013-09-25)routes/(urn:opendaylight:params:xml:ns:yang:bgp-inet?revision=2015-03-05)ipv4-routes/ipv4-route/ipv4-route";
        assertEquals(result, this.testSupport.routePath(routePath, routeId).toString());
    }

    @Test
    public void testDeleteRoutes() {
        final ContainerNode advertised = ImmutableContainerNodeBuilder.create().addChild(this.choiceNode).withNodeIdentifier(new NodeIdentifier(WithdrawnRoutes.QNAME)).build();
        final ContainerNode nlri = ImmutableContainerNodeBuilder.create().addChild(advertised).withNodeIdentifier(new NodeIdentifier(Nlri.QNAME)).build();
        this.testSupport.deleteRoutes(null, null, nlri);
        assertEquals(dest, this.destination);
    }

    @Test
    public void testPutRoutes() {
        final ContainerNode advertised = ImmutableContainerNodeBuilder.create().addChild(this.choiceNode).withNodeIdentifier(new NodeIdentifier(AdvertizedRoutes.QNAME)).build();
        final ContainerNode nlri = ImmutableContainerNodeBuilder.create().addChild(advertised).withNodeIdentifier(new NodeIdentifier(Nlri.QNAME)).build();
        this.testSupport.putRoutes(null, null, nlri, null);
        assertEquals(dest, this.destination);
    }*/
}
