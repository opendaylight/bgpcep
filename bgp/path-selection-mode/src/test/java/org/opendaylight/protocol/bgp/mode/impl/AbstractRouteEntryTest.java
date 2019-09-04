/*
 * Copyright (c) 2019 Lumina Networks, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

public abstract class AbstractRouteEntryTest {

    protected static final RouterId ROUTER_ID = RouterId.forAddress("192.168.254.1");
    protected static final Long ASN = 100L;
    protected static final IpPrefix IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("34.34.34.0/24"));
    protected static final String LABEL_KEY = "route-1";
    protected static final PathId PATH_ID = new PathId(0L);
    protected static final LabeledUnicastRouteKey ROUTE_KEY = new LabeledUnicastRouteKey(PATH_ID, LABEL_KEY);
    protected static final AsPath AS_PATH = new AsPathBuilder().build();
    protected static final Origin BGP_ORIGIN = new OriginBuilder().setValue(BgpOrigin.Igp).build();
    protected static final CNextHop NEXT_HOP = new Ipv4NextHopCaseBuilder()
            .setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("192.168.254.5")).build()).build();

    protected static final Attributes BGP_ATTR1 = new AttributesBuilder().setAsPath(AS_PATH).setOrigin(BGP_ORIGIN)
            .setCNextHop(NEXT_HOP).setLocalPref(new LocalPrefBuilder().setPref(100L).build()).build();
    protected static final Attributes BGP_ATTR2 = new AttributesBuilder().setAsPath(AS_PATH).setOrigin(BGP_ORIGIN)
            .setCNextHop(NEXT_HOP).setLocalPref(new LocalPrefBuilder().setPref(200L).build()).build();

    protected static final List<LabelStack> LABEL_STACK1 =
            Lists.newArrayList(new LabelStackBuilder().setLabelValue(new MplsLabel(90004L)).build());
    protected static final List<LabelStack> LABEL_STACK2 = Lists.newArrayList(
            new LabelStackBuilder().setLabelValue(new MplsLabel(90003L)).setLabelValue(new MplsLabel(90005L)).build());

    protected static final LabeledUnicastRoute ROUTE_LS1_ATTR1 = new LabeledUnicastRouteBuilder().withKey(ROUTE_KEY)
            .setPrefix(IPV4_PREFIX).setPathId(PATH_ID).setLabelStack(LABEL_STACK1).setAttributes(BGP_ATTR1).build();
    protected static final LabeledUnicastRoute ROUTE_LS1_ATTR2 = new LabeledUnicastRouteBuilder().withKey(ROUTE_KEY)
            .setPrefix(IPV4_PREFIX).setPathId(PATH_ID).setLabelStack(LABEL_STACK1).setAttributes(BGP_ATTR2).build();
    protected static final LabeledUnicastRoute ROUTE_LS2_ATTR1 = new LabeledUnicastRouteBuilder().withKey(ROUTE_KEY)
            .setPrefix(IPV4_PREFIX).setPathId(PATH_ID).setLabelStack(LABEL_STACK2).setAttributes(BGP_ATTR1).build();

    @Test
    public void testBestRouteSelectionAfterRouteUpdate() {
        RouteEntry<LabeledUnicastRoutesCase, LabeledUnicastRoutes, LabeledUnicastRoute,
            LabeledUnicastRouteKey> entry;

        // add the same route again and verify that best route is not changed
        entry = createRouteEntry();
        entry.addRoute(ROUTER_ID, PATH_ID.getValue(), ROUTE_LS1_ATTR1);
        assertTrue(entry.selectBest(ASN));
        entry.addRoute(ROUTER_ID, PATH_ID.getValue(), ROUTE_LS1_ATTR1);
        assertFalse(entry.selectBest(ASN));

        // add route with different attributes and verify that best route gets changed
        entry = createRouteEntry();
        entry.addRoute(ROUTER_ID, PATH_ID.getValue(), ROUTE_LS1_ATTR1);
        assertTrue(entry.selectBest(ASN));
        entry.addRoute(ROUTER_ID, PATH_ID.getValue(), ROUTE_LS1_ATTR2);
        assertTrue(entry.selectBest(ASN));

        // add route with same attributes but different label-stack and verify that best
        // route gets changed
        entry = createRouteEntry();
        entry.addRoute(ROUTER_ID, PATH_ID.getValue(), ROUTE_LS1_ATTR1);
        assertTrue(entry.selectBest(ASN));
        entry.addRoute(ROUTER_ID, PATH_ID.getValue(), ROUTE_LS2_ATTR1);
        assertTrue(entry.selectBest(ASN));
        entry.addRoute(ROUTER_ID, PATH_ID.getValue(), ROUTE_LS2_ATTR1);
        assertFalse(entry.selectBest(ASN));
    }

    protected abstract <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
        RouteEntry<C, S, R, I> createRouteEntry();
}
