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

import com.google.common.io.Resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.GlobalAfiSafiStateAugmentationBuilder;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint32;

public class GlobalStateCliUtilsTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(output);

    @Test
    public void testEmptyGlobalStateCliUtil() throws IOException {
        final GlobalBuilder builder = buildGlobal(false);
        GlobalStateCliUtils.displayRibOperationalState(RIB_ID, builder.build(), stream);

        final String expected = Resources.toString(getClass().getClassLoader().getResource("empty-global.txt"),
            StandardCharsets.UTF_8);
        assertEquals(expected, output.toString());
    }

    @Test
    public void testGlobalStateCliUtil() throws IOException {
        final GlobalBuilder builder = buildGlobal(true);
        GlobalStateCliUtils.displayRibOperationalState(RIB_ID, builder.build(), stream);

        final String expected = Resources.toString(getClass().getClassLoader().getResource("global.txt"),
            StandardCharsets.UTF_8);
        assertEquals(expected, output.toString());
    }

    static GlobalBuilder buildGlobal(final boolean withStateAug) {
        final GlobalBuilder builder = new GlobalBuilder().setState(new StateBuilder()
                .setAs(AsNumber.getDefaultInstance("100"))
                .setTotalPaths(Uint32.ONE)
                .setTotalPrefixes(Uint32.TWO)
                .build());

        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp
                .common.afi.safi.list.afi.safi.StateBuilder stateBuilder =
                new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp
                        .common.afi.safi.list.afi.safi.StateBuilder();
        if (withStateAug) {
            stateBuilder.addAugmentation(new GlobalAfiSafiStateAugmentationBuilder()
                .setTotalPaths(Uint32.valueOf(3))
                .setTotalPrefixes(Uint32.valueOf(4))
                .build());
        }

        return builder
                .setAfiSafis(new AfiSafisBuilder()
                    .setAfiSafi(BindingMap.of(new AfiSafiBuilder()
                        .setAfiSafiName(IPV4UNICAST.VALUE)
                        .setState(stateBuilder.build())
                        .build()))
                    .build());
    }
}