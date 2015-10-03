/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

/**
 * Provides hook methods for tracking the BGP configuration module instance life-cycle.
 *
 */
public interface BGPConfigModuleTracker {

    /**
     * Provides an action when configuration module instance is created.
     */
    void onInstanceCreate();

    /**
     *  Provides an action when configuration module instance is closed.
     */
    void onInstanceClose();

}
