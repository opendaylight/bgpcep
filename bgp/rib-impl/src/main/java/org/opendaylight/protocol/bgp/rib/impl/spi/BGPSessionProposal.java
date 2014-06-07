/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

/**
 * Interface that provides the initial acceptable session characteristics with which the session should be started.
 */
public interface BGPSessionProposal {
    /**
     * Returns BGPSessionPreferences for this IP address.
     *
     * @param address serves as constraint, the implementation can also take time into consideration
     * @return BGPSessionPreferences with acceptable session characteristics
     */
    BGPSessionPreferences getProposal();
}
