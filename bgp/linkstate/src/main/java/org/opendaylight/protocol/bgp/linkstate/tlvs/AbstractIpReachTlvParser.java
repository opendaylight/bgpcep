/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public abstract class AbstractIpReachTlvParser implements LinkstateTlvSerializer<IpPrefix>, LinkstateTlvParser<IpPrefix> {

    private static final int IP_REACHABILITY = 265;

    public static final QName IP_REACHABILITY_QNAME = QName.create(PrefixDescriptors.QNAME, "ip-reachability-information").intern();

    @Override
    public final void serializeTlvBody(final IpPrefix tlv, final ByteBuf body) {
        if (tlv.getIpv4Prefix() != null) {
            ByteBufWriteUtil.writeMinimalPrefix(tlv.getIpv4Prefix(), body);
        } else if (tlv.getIpv6Prefix() != null) {
            ByteBufWriteUtil.writeMinimalPrefix(tlv.getIpv6Prefix(), body);
        }
    }

    @Override
    public final int getType() {
        return IP_REACHABILITY;
    }
}
