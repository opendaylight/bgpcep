/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class RibSupportUtilsTest {

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<RibSupportUtils> c = RibSupportUtils.class.getDeclaredConstructor(null);
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testYangTablesKey() {
        final Class<? extends AddressFamily> afi = Ipv4AddressFamily.class;
        final Class<? extends SubsequentAddressFamily> safi = UnicastSubsequentAddressFamily.class;
        final TablesKey k = new TablesKey(afi, safi);
        final NodeIdentifierWithPredicates p = RibSupportUtils.toYangTablesKey(k);
        final Map<QName, Object> m = p.getKeyValues();
        assertFalse(m.isEmpty());
        assertEquals(Tables.QNAME, p.getNodeType());
        assertTrue(m.containsValue(BindingReflections.findQName(afi)));
        assertTrue(m.containsValue(BindingReflections.findQName(safi)));
    }

    @Test
    public void testYangKey() {
        final Class<? extends AddressFamily> afi = Ipv4AddressFamily.class;
        final Class<? extends SubsequentAddressFamily> safi = UnicastSubsequentAddressFamily.class;
        final TablesKey k = new TablesKey(afi, safi);
        final NodeIdentifierWithPredicates p = RibSupportUtils.toYangKey(SupportedTables.QNAME, k);
        final Map<QName, Object> m = p.getKeyValues();
        assertFalse(m.isEmpty());
        assertEquals(SupportedTables.QNAME, p.getNodeType());
        assertTrue(m.containsValue(BindingReflections.findQName(afi)));
        assertTrue(m.containsValue(BindingReflections.findQName(safi)));
    }
}
