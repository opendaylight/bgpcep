/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;
import com.google.common.collect.ImmutableCollection;
import java.util.Collection;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableChoiceNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

public class AbstractRIBSupportTest {

    private final ContainerNode ipv4p = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(Ipv4Prefixes.QNAME)).build();
    private final ContainerNode destination = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(DestinationIpv4.QNAME)).addChild(this.ipv4p).build();
    private final ChoiceNode choiceNode = ImmutableChoiceNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(DestinationType.QNAME)).addChild(this.destination).build();

    static ContainerNode dest;

    private final RIBSupport testSupport = new AbstractRIBSupport(Ipv4RoutesCase.class, Ipv4Routes.class, Ipv4Route.class) {

        @Override
        public ChoiceNode emptyRoutes() {
            return null;
        }

        @Override
        public ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
            return null;
        }

        @Override
        public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
            return null;
        }

        @Override
        protected NodeIdentifier destinationContainerIdentifier() {
            return new NodeIdentifier(DestinationIpv4.QNAME);
        }

        @Override
        protected void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
            final ContainerNode destination) {
            AbstractRIBSupportTest.dest = destination;
        }

        @Override
        protected void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
            final ContainerNode destination, final ContainerNode attributes) {
            AbstractRIBSupportTest.dest = destination;
        }

        @Override
        public boolean isComplexRoute() {
            return false;
        }

        @Override
        protected MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
            return null;
        }

        @Override
        protected MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
            return null;
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
        // TODO:
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
    }
}
