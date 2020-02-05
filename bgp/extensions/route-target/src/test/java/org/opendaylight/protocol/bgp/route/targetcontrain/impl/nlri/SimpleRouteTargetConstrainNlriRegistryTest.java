/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.NlriActivator;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainDefaultCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainIpv4RouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.route.target.extended.community.grouping.As4RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.spec.common.As4SpecificCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.constrain._default.route.grouping.RouteTargetConstrainDefaultRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.extended.community.grouping.RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.ipv4.grouping.RouteTargetIpv4Builder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(Parameterized.class)
public class SimpleRouteTargetConstrainNlriRegistryTest {
    private static final As4SpecificCommon AS_COMMON = new As4SpecificCommonBuilder()
            .setAsNumber(new AsNumber(Uint32.valueOf(20)))
            .setLocalAdministrator(Uint16.valueOf(100)).build();

    private static final byte[] RT_DEFAULT_BUFF = new byte[0];
    private static final Integer RT_2_OCT_TYPE = 0;
    private static final byte[] RT_2_OCT_BUFF = new byte[]{
        0, 35, 4, 2, 8, 7
    };
    private static final byte[] RT_2_OCT_BUFF_WT = new byte[]{
        0, 2, 0, 35, 4, 2, 8, 7
    };
    private static final Integer RT_IPV4_TYPE = 1;
    private static final byte[] RT_IPV4_BUFF = new byte[]{
        12, 51, 2, 5, 0x15, 0x2d
    };
    private static final byte[] RT_IPV4_BUFF_WT = new byte[]{
        1, 2, 12, 51, 2, 5, 0x15, 0x2d
    };
    private static final Integer RT_4_OCT_TYPE = 2;
    private static final byte[] RT_4_OCT_BUFF = new byte[]{
        0, 0, 0, 20, 0, 100
    };
    private static final byte[] RT_4_OCT_BUFF_WT = new byte[]{
        2, 2, 0, 0, 0, 20, 0, 100
    };
    private static RouteTargetConstrainChoice RT_DEFAULT = new RouteTargetConstrainDefaultCaseBuilder()
            .setRouteTargetConstrainDefaultRoute(new RouteTargetConstrainDefaultRouteBuilder()
                    .build()).build();
    private static RouteTargetConstrainChoice RT_AS_2_OCT = new RouteTargetConstrainRouteCaseBuilder()
            .setRouteTargetExtendedCommunity(new RouteTargetExtendedCommunityBuilder()
                    .setGlobalAdministrator(new ShortAsNumber(Uint32.valueOf(35)))
                    .setLocalAdministrator(new byte[]{4, 2, 8, 7})
                    .build()).build();
    private static RouteTargetConstrainChoice RT_IPV4 = new RouteTargetConstrainIpv4RouteCaseBuilder()
            .setRouteTargetIpv4(new RouteTargetIpv4Builder()
                    .setGlobalAdministrator(new Ipv4AddressNoZone("12.51.2.5"))
                    .setLocalAdministrator(Uint16.valueOf(5421))
                    .build()).build();
    private static RouteTargetConstrainChoice RT_AS_4_OCT = new RouteTargetConstrainAs4ExtendedCommunityCaseBuilder()
            .setAs4RouteTargetExtendedCommunity(new As4RouteTargetExtendedCommunityBuilder()
                    .setAs4SpecificCommon(AS_COMMON).build()).build();

    private final SimpleRouteTargetConstrainNlriRegistry nlriRegistry
            = SimpleRouteTargetConstrainNlriRegistry.getInstance();
    private final Integer type;
    private final byte[] expectedBuffer;
    private final byte[] expectedBufferWithType;
    private final RouteTargetConstrainChoice expected;

    public SimpleRouteTargetConstrainNlriRegistryTest(
            final RouteTargetConstrainChoice routeTargetConstrainChoice,
            final Integer type,
            final byte[] expectedBuffer,
            final byte[] expectedBufferWithType
    ) {
        this.expected = routeTargetConstrainChoice;
        this.type = type;
        this.expectedBuffer = expectedBuffer;
        this.expectedBufferWithType = expectedBufferWithType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {RT_AS_4_OCT, RT_4_OCT_TYPE, RT_4_OCT_BUFF, RT_4_OCT_BUFF_WT},
                {RT_IPV4, RT_IPV4_TYPE, RT_IPV4_BUFF, RT_IPV4_BUFF_WT},
                {RT_AS_2_OCT, RT_2_OCT_TYPE, RT_2_OCT_BUFF, RT_2_OCT_BUFF_WT},
                {RT_DEFAULT, null, RT_DEFAULT_BUFF, RT_DEFAULT_BUFF}
        });
    }

    @Before
    public void setUp() {
        NlriActivator.registerNlriParsers(new ArrayList<>());
    }

    @Test
    public void testHandler() {
        assertEquals(this.expected, this.nlriRegistry.parseRouteTargetConstrain(this.type,
                Unpooled.copiedBuffer(this.expectedBuffer)));
        assertArrayEquals(this.expectedBufferWithType,
                ByteArray.getAllBytes(this.nlriRegistry.serializeRouteTargetConstrain(this.expected)));
    }
}