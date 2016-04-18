/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.nlri;

import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv4ReachTlvParser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yangtools.yang.common.QName;

public final class Ipv4PrefixNlriParser extends AbstractPrefixNlriParser {

    public Ipv4PrefixNlriParser(final SimpleNlriTypeRegistry registry) {
        super(registry);
    }

    @Override
    public int getNlriType() {
        return NlriType.Ipv4Prefix.getIntValue();
    }

    @Override
    protected IpPrefix parseIpReachability(final Map<QName, Object> tlvs) {
        return (IpPrefix) tlvs.get(Ipv4ReachTlvParser.IPV4_REACHABILITY_QNAME);
    }

}
