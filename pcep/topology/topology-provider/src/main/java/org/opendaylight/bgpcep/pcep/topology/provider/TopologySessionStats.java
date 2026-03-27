/*
 * Copyright (c) 2017 AT&T AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

/**
 * Provide access to topology session stats without expose variables.
 */
interface TopologySessionStats {
    /**
     * {@return true if session is synchronized}
     */
    boolean isSessionSynchronized();

    /**
     * {@return true if Initiation Capability is advertised}
     */
    boolean isInitiationCapability();

    /**
     * {@return true if Stateful Capability is advertised}
     */
    boolean isStatefulCapability();

    /**
     * {@return true if Lsp Update Capability is advertised}
     */
    boolean isLspUpdateCapability();

    /**
     * {@return the number of delegated LSPs (tunnels) from PCC}
     */
    int getDelegatedLspsCount();

    /**
     * {@return the update interval (timer in nanos) to run updates}
     */
    long updateInterval();
}
