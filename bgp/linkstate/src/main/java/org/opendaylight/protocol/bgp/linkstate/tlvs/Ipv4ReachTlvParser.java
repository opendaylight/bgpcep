/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public class Ipv4ReachTlvParser extends AbstractIpReachTlvParser {

    public static final QName IPV4_REACHABILITY_QNAME = QName.create(PrefixDescriptors.QNAME, "ip-reachability-information-ipv4").intern();

    @Override
    public IpPrefix parseTlvBody(final ByteBuf value) {
        return new IpPrefix(Ipv4Util.prefixForByteBuf(value));
    }

    @Override
    public QName getTlvQName() {
        return IPV4_REACHABILITY_QNAME;
    }

}
