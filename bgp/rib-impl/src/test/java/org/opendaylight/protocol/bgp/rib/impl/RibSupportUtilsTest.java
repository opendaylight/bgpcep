/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class RibSupportUtilsTest {
    @Test
    public void testYangTablesKey() {
        final NodeIdentifierWithPredicates p = RibSupportUtils.toYangTablesKey(new TablesKey(Ipv4AddressFamily.VALUE,
            UnicastSubsequentAddressFamily.VALUE));
        final Map<QName, Object> m = p.asMap();
        assertFalse(m.isEmpty());
        assertEquals(Tables.QNAME, p.getNodeType());
        assertTrue(m.containsValue(Ipv4AddressFamily.QNAME));
        assertTrue(m.containsValue(UnicastSubsequentAddressFamily.QNAME));
    }
}
