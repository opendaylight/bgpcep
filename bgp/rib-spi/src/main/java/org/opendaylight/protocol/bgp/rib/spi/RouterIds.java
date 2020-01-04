/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;

@NonNullByDefault
public final class RouterIds {
    private RouterIds() {
        // Hidden on purpose
    }

    public static PeerId createPeerId(final IpAddress address) {
        final Ipv4Address ipv4 = address.getIpv4Address();
        return ipv4 != null ? createPeerId(ipv4)
                : new PeerId(RouterId.PEER_ID_PREFIX + address.getIpv6Address().getValue());
    }

    public static PeerId createPeerId(final Ipv4Address address) {
        return new PeerId(RouterId.PEER_ID_PREFIX + address.getValue());
    }
}
