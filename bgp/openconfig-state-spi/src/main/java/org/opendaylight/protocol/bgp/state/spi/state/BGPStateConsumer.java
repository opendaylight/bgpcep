/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi.state;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * OpenConfig BGP State for use by consumers.
 */
public interface BGPStateConsumer {
    /**
     * @return Bgp instance identifier
     */
    InstanceIdentifier<Bgp> getInstanceIdentifier();

    /**
     * OpenConfig BGP State
     *
     * @return Bgp State
     */
    Bgp getBgpState();
}
