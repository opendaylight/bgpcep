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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class RibSupportUtilsTest {
    private static final NodeIdentifierWithPredicates NII;
    private static final NodeIdentifierWithPredicates NII_PATH;
    private static final Class<? extends AddressFamily> AFI = Ipv4AddressFamily.class;
    private static final Class<? extends SubsequentAddressFamily> SAFI = UnicastSubsequentAddressFamily.class;
    private static final TablesKey TABLE_KEY = new TablesKey(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);

    static {
        final QName afi = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-rib?revision=2017-12-07",
                "afi");
        final QName safi = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-rib?revision=2017-12-07",
                "safi");
        NII = new NodeIdentifierWithPredicates(SupportedTables.QNAME, ImmutableMap.of(afi, Ipv4AddressFamily.QNAME,
                safi, UnicastSubsequentAddressFamily.QNAME));
        NII_PATH = new NodeIdentifierWithPredicates(SupportedTables.QNAME,
                ImmutableMap.of(
                        QName.create("urn:opendaylight:params:xml:ns:yang:bgp-multiprotocol?revision=2017-12-07",
                                "afi"), Ipv4AddressFamily.QNAME,
                        QName.create("urn:opendaylight:params:xml:ns:yang:bgp-multiprotocol?revision=2017-12-07",
                                "safi"), UnicastSubsequentAddressFamily.QNAME)
        );
    }

    @Test
    public void testYangTablesKey() {
        final NodeIdentifierWithPredicates p = RibSupportUtils.toYangTablesKey(TABLE_KEY);
        final Map<QName, Object> m = p.getKeyValues();
        assertFalse(m.isEmpty());
        assertEquals(Tables.QNAME, p.getNodeType());
        assertTrue(m.containsValue(BindingReflections.findQName(AFI)));
        assertTrue(m.containsValue(BindingReflections.findQName(SAFI)));
    }

    @Test
    public void testYangKey() {
        final NodeIdentifierWithPredicates p = RibSupportUtils.toYangKey(SupportedTables.QNAME, TABLE_KEY);
        final Map<QName, Object> m = p.getKeyValues();
        assertFalse(m.isEmpty());
        assertEquals(SupportedTables.QNAME, p.getNodeType());
        assertTrue(m.containsValue(BindingReflections.findQName(AFI)));
        assertTrue(m.containsValue(BindingReflections.findQName(SAFI)));
    }

    @Test
    public void toYangPathKey() {
        final NodeIdentifierWithPredicates result = RibSupportUtils.toYangPathKey(SupportedTables.QNAME, AFI, SAFI);
        assertEquals(NII_PATH.toString(), result.toString());

    }

    @Test
    public void toYangKey() {
        final NodeIdentifierWithPredicates result = RibSupportUtils.toYangKey(SupportedTables.QNAME, TABLE_KEY);
        assertEquals(NII.toString(), result.toString());
    }
}
