/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.protocol.bgp.rib.spi.state;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

/**
 * BGP Operational Transport State.
 */
public interface BGPTransportState {
    /**
     * Local Port.
     *
     * @return port
     */
    @Nonnull
    PortNumber getLocalPort();

    /**
     * Remote Address.
     *
     * @return IpAddress
     */
    @Nonnull
    IpAddress getRemoteAddress();

    /**
     * Remote Port.
     *
     * @return port
     */
    @Nonnull
    PortNumber getRemotePort();
}
