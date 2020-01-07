/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.state;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

/**
 * BGP Operational Transport State.
 */
// FIXME: this could be YANG-modeled
@NonNullByDefault
public interface BGPTransportState {
    /**
     * Local Port.
     *
     * @return port
     */
    PortNumber getLocalPort();

    /**
     * Remote Address.
     *
     * @return IpAddress
     */
    IpAddressNoZone getRemoteAddress();

    /**
     * Remote Port.
     *
     * @return port
     */
    PortNumber getRemotePort();
}
