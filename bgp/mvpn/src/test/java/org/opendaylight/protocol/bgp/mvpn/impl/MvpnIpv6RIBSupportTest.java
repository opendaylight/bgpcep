/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.bgp.rib.rib.loc.rib.tables.routes.MvpnRoutesIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.bgp.rib.rib.loc.rib.tables.routes.MvpnRoutesIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.mvpn.destination.MvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.mvpn.destination.MvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv6AdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv6AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.mvpn.ipv6.advertized._case.DestinationMvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationMvpnIpv6WithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationMvpnIpv6WithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.inter.as.i.pmsi.a.d.grouping.InterAsIPmsiADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.InterAsIPmsiADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.MvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.MvpnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.mvpn.routes.MvpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.mvpn.routes.MvpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.mvpn.routes.MvpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisher;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public final class MvpnIpv6RIBSupportTest extends AbstractRIBSupportTest<MvpnRoute> {
    private static final MvpnRouteKey ROUTE_KEY;
    private static final MvpnRoute ROUTE;
    private static final IpPrefix PREFIX = new IpPrefix(new Ipv6Prefix("2001:db8:1:1::/64"));
    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn
            .routes.MvpnRoutes MVPN_ROUTES;
    private static final MvpnChoice MVPN = new InterAsIPmsiADCaseBuilder().setInterAsIPmsiAD(
            new InterAsIPmsiADBuilder()
                    .setSourceAs(new AsNumber(1L))
                    .setRouteDistinguisher(new RouteDistinguisher(new RdIpv4("1.2.3.4:258")))
                    .build()).build();
    private static final PathId PATH_ID = new PathId(0L);
    private static final MvpnDestination MVPN_DESTINATION = new MvpnDestinationBuilder()
            .setMvpnChoice(MVPN)
            .setIpv6Prefix(PREFIX.getIpv6Prefix())
            .setPathId(PATH_ID)
            .build();
    private static final DestinationMvpnIpv6AdvertizedCase REACH_NLRI = new DestinationMvpnIpv6AdvertizedCaseBuilder()
            .setDestinationMvpn(new DestinationMvpnBuilder()
                    .setMvpnDestination(Collections.singletonList(MVPN_DESTINATION)).build()).build();
    private static final DestinationMvpnIpv6WithdrawnCase UNREACH_NLRI = new DestinationMvpnIpv6WithdrawnCaseBuilder()
            .setDestinationMvpn(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6
                    .rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.mvpn
                    .ipv6.withdrawn._case.DestinationMvpnBuilder()
                    .setMvpnDestination(Collections.singletonList(MVPN_DESTINATION)).build()).build();

    static {
        final ByteBuf buffer = Unpooled.buffer();
        ROUTE_KEY = new MvpnRouteKey(PATH_ID, ByteArray.encodeBase64(buffer));
        ROUTE = new MvpnRouteBuilder()
                .setRouteKey(ROUTE_KEY.getRouteKey())
                .setPrefix(PREFIX)
                .setPathId(ROUTE_KEY.getPathId())
                .setAttributes(ATTRIBUTES)
                .setMvpnChoice(MVPN)
                .build();
        MVPN_ROUTES = new MvpnRoutesBuilder().setMvpnRoute(Collections.singletonList(ROUTE)).build();
    }

    private MvpnIpv6RIBSupport ribSupport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ribSupport = MvpnIpv6RIBSupport.getInstance(this.mappingService);
        setUpTestCustomizer(ribSupport);
    }

    @Test
    public void testEmptyRoute() {
        final Routes empty = new MvpnRoutesIpv6CaseBuilder().setMvpnRoutes(new MvpnRoutesBuilder()
                .setMvpnRoute(Collections.emptyList()).build()).build();
        final ChoiceNode expected = createRoutes(empty);
        assertEquals(expected, this.ribSupport.emptyRoutes());
    }

    @Test
    public void testCacheableNlriObjects() {
        assertEquals(ImmutableSet.of(), this.ribSupport.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        assertEquals(ImmutableSet.of(), this.ribSupport.cacheableAttributeObjects());
    }

    @Test
    public void testRouteIdAddPath() {
        assertEquals(ROUTE_KEY, this.ribSupport.createRouteListKey(ROUTE_KEY.getPathId().getValue(),
                ROUTE_KEY.getRouteKey()));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(
                        Attributes.QNAME.withModule(BindingReflections.getQNameModule(MvpnRoutesIpv6Case.class))),
                this.ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(MvpnRoutesIpv6Case.class, this.ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(MvpnRoutes.class, this.ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(MvpnRoute.class, this.ribSupport.routesListClass());
    }
}