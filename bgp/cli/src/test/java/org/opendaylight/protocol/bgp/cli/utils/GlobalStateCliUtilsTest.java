/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.cli.utils.BGPOperationalStateUtilsTest.RIB_ID;
import static org.opendaylight.protocol.bgp.cli.utils.PeerGroupStateCliUtilsTest.UTF8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.GlobalAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.GlobalAfiSafiStateAugmentationBuilder;

public class GlobalStateCliUtilsTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(this.output);

    @Test
    public void testEmptyGlobalStateCliUtil() throws IOException {
        final GlobalBuilder builder = buildGlobal(false);
        GlobalStateCliUtils.displayRibOperationalState(RIB_ID, builder.build(), this.stream);

        final String expected = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("empty-global.txt"), UTF8);
        assertEquals(expected, this.output.toString());
    }

    @Test
    public void testGlobalStateCliUtil() throws IOException {
        final GlobalBuilder builder = buildGlobal(true);
        GlobalStateCliUtils.displayRibOperationalState(RIB_ID, builder.build(), this.stream);

        final String expected = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("global.txt"), UTF8);
        assertEquals(expected, this.output.toString());
    }

    static GlobalBuilder buildGlobal(final boolean withStateAug) {
        final GlobalBuilder builder = new GlobalBuilder().setState(new StateBuilder()
                .setAs(AsNumber.getDefaultInstance("100"))
                .setTotalPaths(1L)
                .setTotalPrefixes(2L)
                .build());

        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp
                .common.afi.safi.list.afi.safi.StateBuilder stateBuilder =
                new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp
                        .common.afi.safi.list.afi.safi.StateBuilder();
        if (withStateAug) {
            stateBuilder.addAugmentation(GlobalAfiSafiStateAugmentation.class,
                    new GlobalAfiSafiStateAugmentationBuilder().setTotalPaths(3L).setTotalPrefixes(4L).build());
        }


        builder.setAfiSafis(new AfiSafisBuilder()
                .setAfiSafi(Collections.singletonList(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
                        .setState(stateBuilder.build()).build())).build());
        return builder;
    }
}