/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;

public final class RouterIds {
    private RouterIds() {
        throw new UnsupportedOperationException();
    }

    public static PeerId createPeerId(@Nonnull final IpAddress address) {
        if (address.getIpv4Address() != null) {
            return createPeerId(address.getIpv4Address());
        }
        return new PeerId(RouterId.PEER_ID_PREFIX + address.getIpv6Address().getValue());
    }

    public static PeerId createPeerId(@Nonnull final Ipv4Address address) {
        return new PeerId(RouterId.PEER_ID_PREFIX + address.getValue());
    }
}
