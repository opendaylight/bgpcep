/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.target.impl.nlri;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.protocol.bgp.route.target.impl.activators.NlriActivator;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.RouteTargetChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetDefaultCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetIpv4RouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.route.target._default._case.RouteTargetDefaultRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.as._4.route.target.extended.community.grouping.As4RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.as._4.spec.common.As4SpecificCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.route.target.extended.community.grouping.RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.route.target.ipv4.grouping.RouteTargetIpv4Builder;

@RunWith(Parameterized.class)
public class SimpleRouteTargetNlriRegistryTest {
    private static final As4SpecificCommon AS_COMMON = new As4SpecificCommonBuilder()
            .setAsNumber(new AsNumber(20L))
            .setLocalAdministrator(100).build();

    private static final byte[] RT_DEFAULT_BUFF = new byte[0];
    private static final Integer RT_2_OCT_TYPE = 0;
    private static final byte[] RT_2_OCT_BUFF = new byte[]{
            0, 35, 4, 2, 8, 7
    };
    private static final Integer RT_IPV4_TYPE = 1;
    private static final byte[] RT_IPV4_BUFF = new byte[]{
            12, 51, 2, 5, 0x15, 0x2d
    };
    private static final Integer RT_4_OCT_TYPE = 2;
    private static final byte[] RT_4_OCT_BUFF = new byte[]{
            0, 0, 0, 20, 0, 100
    };
    private static RouteTargetChoice RT_DEFAULT = new RouteTargetDefaultCaseBuilder()
            .setRouteTargetDefaultRoute(new RouteTargetDefaultRouteBuilder()
                    .build()).build();
    private static RouteTargetChoice RT_AS_2_OCT = new RouteTargetRouteCaseBuilder()
            .setRouteTargetExtendedCommunity(new RouteTargetExtendedCommunityBuilder()
                    .setGlobalAdministrator(new ShortAsNumber(35L))
                    .setLocalAdministrator(new byte[]{4, 2, 8, 7})
                    .build()).build();
    private static RouteTargetChoice RT_IPV4 = new RouteTargetIpv4RouteCaseBuilder()
            .setRouteTargetIpv4(new RouteTargetIpv4Builder()
                    .setGlobalAdministrator(new Ipv4Address("12.51.2.5"))
                    .setLocalAdministrator(5421)
                    .build()).build();
    private static RouteTargetChoice RT_AS_4_OCT = new RouteTargetAs4ExtendedCommunityCaseBuilder()
            .setAs4RouteTargetExtendedCommunity(new As4RouteTargetExtendedCommunityBuilder()
                    .setAs4SpecificCommon(AS_COMMON).build()).build();

    private final SimpleRouteTargetNlriRegistry nlriRegistry = SimpleRouteTargetNlriRegistry.getInstance();
    private final Integer type;
    private final byte[] expectedBuffer;
    private RouteTargetChoice expected;

    public SimpleRouteTargetNlriRegistryTest(final RouteTargetChoice routeTargetChoice, final Integer type,
            final byte[] expectedBuffer) {
        this.expected = routeTargetChoice;
        this.type = type;
        this.expectedBuffer = expectedBuffer;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {RT_AS_4_OCT, RT_4_OCT_TYPE, RT_4_OCT_BUFF},
                {RT_IPV4, RT_IPV4_TYPE, RT_IPV4_BUFF},
                {RT_AS_2_OCT, RT_2_OCT_TYPE, RT_2_OCT_BUFF},
                {RT_DEFAULT, null, RT_DEFAULT_BUFF}
        });
    }

    @Before
    public void setUp() {
        NlriActivator.registerNlriParsers(new ArrayList<>());
    }

    @Test
    public void testHandler() {
        final ByteBuf buff = Unpooled.copiedBuffer(this.expectedBuffer);
        assertEquals(this.expected, this.nlriRegistry.parseRouteTarget(this.type, buff));
        assertArrayEquals(this.expectedBuffer,
                ByteArray.getAllBytes(this.nlriRegistry.serializeRouteTarget(this.expected)));
    }
}