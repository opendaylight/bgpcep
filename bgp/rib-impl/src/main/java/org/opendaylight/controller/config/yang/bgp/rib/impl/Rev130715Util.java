/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

public final class Rev130715Util {
    private Rev130715Util() {
        throw new UnsupportedOperationException();
    }

    public static Ipv4Address getIpv4Address(final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address ipv4) {
        if (ipv4 == null) {
            return null;
        }
        return new Ipv4Address(ipv4.getValue());
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber getASNumber(final Long localAs) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber(localAs);
    }

    public static PortNumber getPort(final Integer port) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber(port);
    }

    public static IpAddress getIpvAddress(final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress hostWithoutValue) {
        if (hostWithoutValue.getIpv4Address() != null) {
            return new IpAddress(new Ipv4Address(hostWithoutValue.getIpv4Address().getValue()));
        }
        return new IpAddress(new Ipv6Address(hostWithoutValue.getIpv6Address().getValue()));
    }
}
