/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state.peer;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;

/**
 * Expose Prefixes Installed Count
 */
public interface PrefixesInstalledCounters {
    /**
     * Prefixes received and installed from Peer count (EffRibIn Count) pet Table
     * @param tablesKey table
     * @return count
     */
    long getPrefixedInstalledCount(@Nonnull final TablesKey tablesKey);

    /**
     * total Prefixes received and installed from Peer count (EffRibIn Count)
     * @return count
     */
    long getTotalPrefixesInstalled();
}
