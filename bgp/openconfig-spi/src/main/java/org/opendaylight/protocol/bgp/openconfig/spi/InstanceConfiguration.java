/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

/**
 * Common interface for all BGP configuration holders
 *
 */
public interface InstanceConfiguration {

    /**
     * Returns a name of BGP configuration module instance
     * @return name of BGP configuration
     */
    String getInstanceName();

}
