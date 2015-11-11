/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.util;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.getModuleName;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.toAfiSafi;
import com.google.common.collect.Lists;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class OpenConfigUtilTest {

    private static final String TEST = "/modules/module[type='dom-concurrent-data-broker'][name='concurrent-data-broker']";

    @Test
    public void test() {
        final String moduleName = getModuleName(TEST);
        assertEquals("concurrent-data-broker", moduleName);
    }

    @Test
    public void testToAfiSafi() {
        assertEquals(toAfiSafi(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)).get(),
                new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build());
    }

    @Test
    public void testToAfiSafis() {
        final List<AfiSafi> afiSafis = OpenConfigUtil.toAfiSafis(Lists.<BgpTableType>newArrayList(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)));
        Assert.assertEquals(Collections.singletonList(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build()), afiSafis);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<OpenConfigUtil> c = OpenConfigUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

}