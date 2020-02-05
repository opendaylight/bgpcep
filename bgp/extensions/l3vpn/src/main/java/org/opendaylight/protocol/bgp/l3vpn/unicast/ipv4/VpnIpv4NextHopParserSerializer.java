/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4;

import org.opendaylight.protocol.bgp.l3vpn.unicast.AbstractVpnNextHopParserSerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;

public final class VpnIpv4NextHopParserSerializer extends AbstractVpnNextHopParserSerializer {
    public VpnIpv4NextHopParserSerializer() {
        super(Ipv4Util.IP4_LENGTH, Ipv4NextHopCase.class);
    }
}
