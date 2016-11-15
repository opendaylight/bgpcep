/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.state.spi.state.BGPStateConsumer;
import org.opendaylight.protocol.concepts.AbstractRegistration;

/**
 * Provides OpenConfig BGP state.
 */
public interface BGPStateProvider {
    /**
     * Register BGP State
     *
     * @param bgpStateConsumer State Consumer
     */
    @Nonnull AbstractRegistration registerBGPState(@Nonnull BGPStateConsumer bgpStateConsumer);
}
