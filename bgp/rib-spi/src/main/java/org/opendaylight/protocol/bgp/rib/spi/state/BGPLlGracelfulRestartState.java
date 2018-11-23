/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.state;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;

/**
 * BGP Operational Graceful Restart State.
 */
public interface BGPLlGracelfulRestartState extends BGPGracelfulRestartState {
    /**
     * is Long-lived Graceful Restart Support advertised to neighbor.
     *
     * @param tablesKey tables Key
     * @return true if Afi Safi was advertised to neighbor
     */
    boolean isLlGracefulRestartAdvertised(@Nonnull TablesKey tablesKey);

    /**
     * is Long-lived Graceful Restart Support advertised by neighbor.
     *
     * @param tablesKey tables Key
     * @return true if Afi Safi was advertised by neighbor
     */
    boolean isLlGracefulRestartReceived(@Nonnull TablesKey tablesKey);

    /**
     * If table is both advertised and received return timer with lower value.
     * If table is not advertised or received return zero.
     *
     * @param tablesKey tables key
     * @return effective value of timer in seconds
     */
    int getLlGracefulRestartTimer(@Nonnull TablesKey tablesKey);
}
