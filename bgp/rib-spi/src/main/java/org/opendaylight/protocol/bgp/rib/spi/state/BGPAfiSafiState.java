/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;

/**
 * BGP Operational Afi SafiS State
 */
public interface BGPAfiSafiState extends BGPGracelfulRestartState {
    /**
     * is AfiSafi Supported
     *
     * @param tablesKey tables Key
     * @return true if Afi Safi was advertized to and by the neighbor
     */
    boolean isAfiSafiSupported(@Nonnull TablesKey tablesKey);

    /**
     * Prefixes installed per specific tablekey
     *
     * @param tablesKey tables Key
     * @return count
     */
    long getPrefixesInstalledCount(@Nonnull TablesKey tablesKey);

    /**
     * Prefixed sent to the Peer count
     *
     * @param tablesKey tablesKey Type
     * @return Prefixes sent count
     */
    long getPrefixesSentCount(@Nonnull TablesKey tablesKey);

    /**
     * Prefixed received from the peer count
     *
     * @param tablesKey tablesKey Type
     * @return Prefixed received count
     */
    long getPrefixesReceivedCount(@Nonnull TablesKey tablesKey);

    /**
     * List of TablesKey - Afi Safi Advertized to the neighbor
     *
     * @return TableKeys
     */
    @Nonnull
    Set<TablesKey> getAfiSafisAdvertized();

    /**
     * List of TablesKey - Afi Safi Advertized by neighbor
     *
     * @return TableKeys
     */
    @Nonnull
    Set<TablesKey> getAfiSafisReceived();
}
