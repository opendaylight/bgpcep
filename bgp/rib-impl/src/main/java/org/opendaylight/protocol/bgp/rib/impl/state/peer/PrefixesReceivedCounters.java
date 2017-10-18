/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state.peer;

import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;

/**
 * Expose Prefixes Received Count
 */
public interface PrefixesReceivedCounters {
    /**
     * Prefixes received from Peer count (AdjRinIn Count) pet Table
     * @param tablesKey table
     * @return count
     */
    long getPrefixedReceivedCount(@Nonnull final TablesKey tablesKey);

    /**
     * list of supported tables per Peer
     * @return tables list
     */
    Set<TablesKey> getTableKeys();

    /**
     * table supported per Peer
     * @param tablesKey table type
     * @return true if supported
     */
    boolean isSupported(TablesKey tablesKey);
}
