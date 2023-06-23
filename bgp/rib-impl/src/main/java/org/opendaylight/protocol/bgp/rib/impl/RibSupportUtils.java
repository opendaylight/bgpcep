/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.ImmutableList;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.util.ImmutableOffsetMapTemplate;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

final class RibSupportUtils {
    private static final ImmutableOffsetMapTemplate<QName> AFI_SAFI_TEMPLATE =
            ImmutableOffsetMapTemplate.ordered(ImmutableList.of(QName.create(Tables.QNAME, "afi").intern(),
                QName.create(Tables.QNAME, "safi").intern()));

    private RibSupportUtils() {
        // Hidden on purpose
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied {@link TablesKey}.
     *
     * @param tablesKey Tables key representing table.
     * @return NodeIdentifierWithPredicates of {@link Tables} for specified AFI, SAFI combination.
     */
    static NodeIdentifierWithPredicates toYangTablesKey(final TablesKey tablesKey) {
        return toYangKey(Tables.QNAME, tablesKey.getAfi(), tablesKey.getSafi());
    }

    /**
     * Creates Yang Instance Identifier path argument from supplied AFI and SAFI.
     *
     * @param id   QNAME representing node
     * @param afi  Class representing AFI
     * @param safi Class representing SAFI
     * @return NodeIdentifierWithPredicates of 'id' for specified AFI, SAFI combination.
     */
    static NodeIdentifierWithPredicates toYangKey(final QName id, final AddressFamily afi,
            final SubsequentAddressFamily safi) {
        return NodeIdentifierWithPredicates.of(id, AFI_SAFI_TEMPLATE.instantiateWithValues(
            BindingReflections.getQName(afi), BindingReflections.getQName(safi)));
    }
}
