/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.Ipv6SrPrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.Ipv6SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6PrefixSidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6PrefixSidCaseBuilder;

public final class Ipv6PrefixSidParser implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    public static final int IPV6_PREFIX_SID = 1169;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        final Ipv6SrPrefix ipv6Prefix = Ipv6SrPrefixAttributesParser.parseSrIpv6Prefix(slice);
        return new Ipv6PrefixSidCaseBuilder().setAlgorithm(ipv6Prefix.getAlgorithm()).build();
    }

    @Override
    public int getType() {
        return IPV6_PREFIX_SID;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        Preconditions.checkArgument(bindingSubTlv instanceof Ipv6PrefixSidCase, "Wrong BindingSubTlv instance expected", bindingSubTlv);
        final Ipv6PrefixSidCase prefix = (Ipv6PrefixSidCase) bindingSubTlv;
        final ByteBuf buffer = Unpooled.buffer();
        Ipv6SrPrefixAttributesParser.serializePrefixAttributes(prefix.getAlgorithm(), buffer);
        TlvUtil.writeTLV(getType(), buffer, aggregator);
    }
}
