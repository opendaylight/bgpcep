/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.app;

import com.google.common.collect.ImmutableMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.BmpMonitor;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public final class TablesUtil {

    public static final QName BMP_TABLES_QNAME = QName.create(BmpMonitor.QNAME.getNamespace(), BmpMonitor.QNAME.getRevision(), "tables").intern();
    public static final QName BMP_ATTRIBUTES_QNAME = QName.create(BmpMonitor.QNAME.getNamespace(), BmpMonitor.QNAME.getRevision(), "attributes").intern();
    public static final QName BMP_ROUTES_QNAME = QName.create(BmpMonitor.QNAME.getNamespace(), BmpMonitor.QNAME.getRevision(), "routes").intern();
    public static final QName BMP_AFI_QNAME = QName.create(BMP_TABLES_QNAME, "afi").intern();
    public static final QName BMP_SAFI_QNAME = QName.create(BMP_TABLES_QNAME, "safi").intern();

    private TablesUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied AFI and SAFI
     *
     * @param afi Class representing AFI
     * @param safi Class representing SAFI
     * @return NodeIdentifierWithPredicates for specified AFI, SAFI combination.
     */
    public static NodeIdentifierWithPredicates toYangTablesKey(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        final ImmutableMap<QName, Object> keyValues = ImmutableMap.of(
                        BMP_AFI_QNAME, BindingReflections.findQName(afi),
                        BMP_SAFI_QNAME, BindingReflections.findQName(safi));
        return new NodeIdentifierWithPredicates(BMP_TABLES_QNAME, keyValues);
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied QNAMES and AFI and SAFI
     *
     * @param nodeName QName reprenting node
     * @param afi Class representing AFI
     * @param safi Class representing SAFI
     * @return NodeIdentifierWithPredicates for specified AFI, SAFI combination.
     */
    public static NodeIdentifierWithPredicates toYangTablesKey(final QName nodeName, final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        final QName afiQname = QName.create(nodeName, "afi").intern();
        final QName safiQname = QName.create(nodeName, "safi").intern();
        final ImmutableMap<QName, Object> keyValues = ImmutableMap.of(
                        afiQname, BindingReflections.findQName(afi),
                        safiQname, BindingReflections.findQName(safi));
        return new NodeIdentifierWithPredicates(nodeName, keyValues);
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied {@link TablesKey}
     *
     * @param k Tables key representing table.
     * @return NodeIdentifierWithPredicates of for specified AFI, SAFI combination.
     */
    public static NodeIdentifierWithPredicates toYangTablesKey(final TablesKey k) {
        return toYangTablesKey(k.getAfi(), k.getSafi());
    }
}
