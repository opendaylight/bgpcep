/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReachTlvParser implements LinkstateTlvParser.LinkstateTlvSerializer<IpPrefix>,
        LinkstateTlvParser<IpPrefix> {
    public static final QName IP_REACHABILITY_QNAME = QName.create(PrefixDescriptors.QNAME,
        "ip-reachability-information").intern();
    @VisibleForTesting
    public static final NodeIdentifier IP_REACH_NID = NodeIdentifier.create(IP_REACHABILITY_QNAME);
    private static final Logger LOG = LoggerFactory.getLogger(ReachTlvParser.class);
    private static final int IP_REACHABILITY = 265;

    /**
     * Note: IP Reachability TLV serves to transport both an IPV4 or an IPV6 prefix.
     * Only the Length of the IP Reachability TLV could be used to discriminate both type of prefix.
     * However, the Length of the TLV is not transmit to the parser. Thus, only the length of ByteBuf could be used.
     */
    @Override
    public IpPrefix parseTlvBody(final ByteBuf value) {
        /* Get address length to determine if it is an IPV4 or an IPV6 prefix */
        final int length = value.readableBytes() - 1;
        if (length == Ipv4Util.IP4_LENGTH) {
            return new IpPrefix(Ipv4Util.prefixForByteBuf(value));
        } else if (length == Ipv6Util.IPV6_LENGTH) {
            return new IpPrefix(Ipv6Util.prefixForByteBuf(value));
        } else {
            return null;
        }
    }

    @Override
    public QName getTlvQName() {
        return IP_REACHABILITY_QNAME;
    }

    @Override
    public void serializeTlvBody(final IpPrefix tlv, final ByteBuf body) {
        if (tlv.getIpv4Prefix() != null) {
            Ipv4Util.writeMinimalPrefix(tlv.getIpv4Prefix(), body);
        } else if (tlv.getIpv6Prefix() != null) {
            Ipv6Util.writeMinimalPrefix(tlv.getIpv6Prefix(), body);
        }
    }

    @Override
    public int getType() {
        return IP_REACHABILITY;
    }

    public static IpPrefix serializeModel(final ContainerNode prefixDesc) {
        return prefixDesc.findChildByArg(IP_REACH_NID)
                .map(child -> {
                    final String prefix = (String) child.body();
                    /* Get the prefix length from the string to determine if it is an IPV4 or an IPV6 prefix */
                    final int length = Ipv4Util.getPrefixLengthBytes(prefix);
                    if (length == Ipv4Util.IP4_LENGTH) {
                        return new IpPrefix(new Ipv4Prefix(prefix));
                    } else if (length == Ipv6Util.IPV6_LENGTH) {
                        return new IpPrefix(new Ipv6Prefix(prefix));
                    } else {
                        return null;
                    }
                })
                .orElse(null);
    }
}
