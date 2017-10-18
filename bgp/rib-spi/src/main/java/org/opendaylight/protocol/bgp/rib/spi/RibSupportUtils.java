/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.collect.ImmutableMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpAddPathTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.SupportedTablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public final class RibSupportUtils {

    private static final QName AFI_QNAME = QName.create(Tables.QNAME, "afi").intern();
    private static final QName SAFI_QNAME = QName.create(Tables.QNAME, "safi").intern();
    private static final QName ADD_PATH_AFI_QNAME = QName.create(BgpAddPathTableType.QNAME, "afi").intern();
    private static final QName ADD_PATH_SAFI_QNAME = QName.create(BgpAddPathTableType.QNAME, "safi").intern();

    private RibSupportUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied AFI and SAFI
     *
     * @param afi Class representing AFI
     * @param safi Class representing SAFI
     * @return NodeIdentifierWithPredicates of {@link Tables} for specified AFI, SAFI combination.
     */
    public static NodeIdentifierWithPredicates toYangTablesKey(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        return toYangKey(Tables.QNAME, afi, safi);
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied AFI and SAFI
     *
     * @param id QNAME representing node
     * @param afi Class representing AFI
     * @param safi Class representing SAFI
     * @return NodeIdentifierWithPredicates of 'id' for specified AFI, SAFI combination.
     */
    public static NodeIdentifierWithPredicates toYangKey(final QName id, final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        final ImmutableMap<QName, Object> keyValues = ImmutableMap.of(
                        AFI_QNAME, BindingReflections.findQName(afi),
                        SAFI_QNAME, BindingReflections.findQName(safi));
        return new NodeIdentifierWithPredicates(id, keyValues);
    }


    /**
     * Creates Yang Instance Identifier path argument from supplied AFI and SAFI
     *
     * @param id QNAME representing node
     * @param afi Class representing AFI
     * @param safi Class representing SAFI
     * @return NodeIdentifierWithPredicates of 'id' for specified AFI, SAFI combination.
     */
    public static NodeIdentifierWithPredicates toYangPathKey(final QName id, final Class<? extends AddressFamily> afi,
        final Class<? extends SubsequentAddressFamily> safi) {
        final ImmutableMap<QName, Object> keyValues = ImmutableMap.of(
            ADD_PATH_AFI_QNAME, BindingReflections.findQName(afi),
            ADD_PATH_SAFI_QNAME, BindingReflections.findQName(safi));
        return new NodeIdentifierWithPredicates(id, keyValues);
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied {@link TablesKey}
     *
     * @param id QNAME representing node
     * @param k Tables key representing table.
     * @return NodeIdentifierWithPredicates of 'id' for specified AFI, SAFI combination.
     */
    public static NodeIdentifierWithPredicates toYangKey(final QName id, final TablesKey k) {
        return toYangKey(id, k.getAfi(), k.getSafi());
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied {@link TablesKey}
     *
     * @param id QNAME representing node
     * @param k  Add PAth Tables key representing table.
     * @return NodeIdentifierWithPredicates of 'id' for specified AFI, SAFI combination.
     */
    public static NodeIdentifierWithPredicates toYangKey(final QName id, final SupportedTablesKey k) {
        return toYangPathKey(id, k.getAfi(), k.getSafi());
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied {@link TablesKey}
     *
     * @param k Tables key representing table.
     * @return NodeIdentifierWithPredicates of {@link Tables} for specified AFI, SAFI combination.
     */
    public static NodeIdentifierWithPredicates toYangTablesKey(final TablesKey k) {
        return toYangTablesKey(k.getAfi(), k.getSafi());
    }
}
