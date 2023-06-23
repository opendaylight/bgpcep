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
        return NodeIdentifierWithPredicates.of(Tables.QNAME, AFI_SAFI_TEMPLATE.instantiateWithValues(
            BindingReflections.getQName(tablesKey.getAfi()), BindingReflections.getQName(tablesKey.getSafi())));
    }
}
