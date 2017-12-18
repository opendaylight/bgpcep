/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import org.opendaylight.protocol.bgp.l3vpn.AbstractVpnNextHopParserSerializer;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

public final class VpnIpv6NextHopParserSerializer extends AbstractVpnNextHopParserSerializer {
    public VpnIpv6NextHopParserSerializer() {
        super(Ipv6Util.IPV6_LENGTH, Ipv6NextHopCase.class);
    }
}
