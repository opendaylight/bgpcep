/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

import javax.annotation.Nonnull;

/**
 * Interface for acquiring BGP RIB State.
 */
public interface BGPRibStateConsumer {
    /**
     * Returns RIB Operational State.
     *
     * @return BGP RIB State
     */
    @Nonnull
    BGPRibState getRIBState();
}
