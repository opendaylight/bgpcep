/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.labeled.unicast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastRoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.routes.LabeledUnicastRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.routes.list.LabeledUnicastRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.routes.list.LabeledUnicastRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;

public class LabeledUnicastIpv4RIBSupportTest extends AbstractRIBSupportTest {

    private static final IpPrefix IPv4_PREFIX = new IpPrefix(new Ipv4Prefix("34.1.22.0/24"));
    private static final LabeledUnicastIpv4RIBSupport RIB_SUPPORT = LabeledUnicastIpv4RIBSupport.getInstance();
    private static final LabeledUnicastRoute ROUTE;
    private static final LabeledUnicastRoutes ROUTES;
    private static final LabeledUnicastRouteKey ROUTE_KEY;
    private static final String LABEL_KEY;
    private static final PathId PATH_ID = new PathId(1L);
    private static final List<LabelStack> LABEL_STACK = Lists.newArrayList(new LabelStackBuilder().setLabelValue(new MplsLabel(355L)).build());
    private static final List<CLabeledUnicastDestination> LABELED_DESTINATION_LIST = Collections.singletonList(new CLabeledUnicastDestinationBuilder()
        .setPathId(PATH_ID).setLabelStack(LABEL_STACK).setPrefix(IPv4_PREFIX).build());
    private static final DestinationLabeledUnicastCase REACH_NLRI = new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
        new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(LABELED_DESTINATION_LIST).build()).build();
    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.
        DestinationLabeledUnicastCase UNREACH_NLRI = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.
        DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.
        type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(LABELED_DESTINATION_LIST).build()).build();

    static {
        final BGPActivator act = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        act.start(context);
        final ByteBuf buffer = Unpooled.buffer();
        LUNlriParser.serializeNlri(LABELED_DESTINATION_LIST, false, buffer);
        LABEL_KEY = ByteArray.encodeBase64(buffer);
        ROUTE_KEY = new LabeledUnicastRouteKey(PATH_ID, LABEL_KEY);
        ROUTE = new LabeledUnicastRouteBuilder().setKey(ROUTE_KEY).setPrefix(IPv4_PREFIX).setPathId(PATH_ID).setLabelStack(LABEL_STACK)
            .setAttributes(new AttributesBuilder().build()).build();
        ROUTES = new LabeledUnicastRoutesBuilder().setLabeledUnicastRoute(Collections.singletonList(ROUTE)).build();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setUpTestCustomizer(RIB_SUPPORT);
    }

    @Test
    public void testDeleteRoutes() {
        RIB_SUPPORT.deleteRoutes(this.tx, getTablePath(), createNlriWithDrawnRoute(UNREACH_NLRI));
        final InstanceIdentifier<LabeledUnicastRoute> instanceIdentifier = (InstanceIdentifier<LabeledUnicastRoute>) this.deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(LabeledUnicastRoute.class));
    }

    @Test
    public void testPutRoutes() {
        RIB_SUPPORT.putRoutes(this.tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final LabeledUnicastRoute route = (LabeledUnicastRoute) this.insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }

    @Test
    public void testEmptyRoute() throws Exception {
        final Routes empty = new LabeledUnicastRoutesCaseBuilder().setLabeledUnicastRoutes(
            new LabeledUnicastRoutesBuilder().setLabeledUnicastRoute(Collections.emptyList()).build()).build();
        final ChoiceNode emptyRoutes = RIB_SUPPORT.emptyRoutes();
        assertEquals(createRoutes(empty), emptyRoutes);
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Update update = RIB_SUPPORT.buildUpdate(Collections.emptyList(), createRoutes(ROUTES), ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().getAugmentation(Attributes2.class)
            .getMpUnreachNlri().getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().getAugmentation(Attributes1.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Update update = RIB_SUPPORT.buildUpdate(createRoutes(ROUTES), Collections.emptyList(), ATTRIBUTES);
        assertEquals(REACH_NLRI, update.getAttributes().getAugmentation(Attributes1.class).getMpReachNlri().getAdvertizedRoutes().getDestinationType());
        assertNull(update.getAttributes().getAugmentation(Attributes2.class));
    }

    @Test
    public void testIsComplexRoute() {
        Assert.assertTrue(RIB_SUPPORT.isComplexRoute());
    }

    @Test
    public void testCacheableNlriObjects() {
        Assert.assertEquals(ImmutableSet.of(), RIB_SUPPORT.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        Assert.assertEquals(ImmutableSet.of(), RIB_SUPPORT.cacheableAttributeObjects());
    }

    @Test
    public void testRouteIdAddPath() {
        final NodeIdentifierWithPredicates expected = createRouteNIWP(ROUTES);
        final NodeIdentifierWithPredicates prefixNii = new NodeIdentifierWithPredicates(RIB_SUPPORT.routeQName(),
            ImmutableMap.of(RIB_SUPPORT.routeKeyQName(), LABEL_KEY));
        Assert.assertEquals(expected, RIB_SUPPORT.getRouteIdAddPath(AbstractRIBSupportTest.PATH_ID, prefixNii));
    }

    @Test
    public void testRoutePath() {
        final NodeIdentifierWithPredicates prefixNii = createRouteNIWP(ROUTES);
        Assert.assertEquals(getRoutePath().node(prefixNii), RIB_SUPPORT.routePath(getTablePath().node(Routes.QNAME), prefixNii));
    }

    @Test
    public void testExtractPathId() {
        Assert.assertEquals((Long) NON_PATH_ID, RIB_SUPPORT.extractPathId(null));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        Assert.assertEquals(new NodeIdentifier(QName.create(LabeledUnicastRoutes.QNAME,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Attributes.QNAME.getLocalName().intern())),
            RIB_SUPPORT.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        Assert.assertEquals(LabeledUnicastRoutesCase.class, RIB_SUPPORT.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        Assert.assertEquals(LabeledUnicastRoutes.class, RIB_SUPPORT.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        Assert.assertEquals(LabeledUnicastRoute.class, RIB_SUPPORT.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new LabeledUnicastRoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyCase)).getRootNode();
        Assert.assertTrue(RIB_SUPPORT.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new LabeledUnicastRoutesCaseBuilder().setLabeledUnicastRoutes(new LabeledUnicastRoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        Assert.assertTrue(RIB_SUPPORT.changedRoutes(tree).isEmpty());

        final Routes routes = new LabeledUnicastRoutesCaseBuilder().setLabeledUnicastRoutes(ROUTES).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = RIB_SUPPORT.changedRoutes(tree);
        Assert.assertFalse(result.isEmpty());
    }
}
