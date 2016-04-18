/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IpReachTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(IpReachTlvParser.class);

    private static final int IP_REACHABILITY = 265;

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Parsing Prefix Descriptor: {}", ByteBufUtil.hexDump(value));
        }
        final PrefixDescriptorsBuilder prefixbuilder = context.getPrefixDescriptorsBuilder();
        if (nlriType.equals(NlriType.Ipv4Prefix)) {
            final IpPrefix prefix = new IpPrefix(Ipv4Util.prefixForByteBuf(value));
            prefixbuilder.setIpReachabilityInformation(prefix);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsed IPv4 reachability info: {}", prefix);
            }
        } else if (nlriType.equals(NlriType.Ipv6Prefix)) {
            final IpPrefix prefix = new IpPrefix(Ipv6Util.prefixForByteBuf(value));
            prefixbuilder.setIpReachabilityInformation(prefix);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsed IPv6 reachability info: {}", prefix);
            }
        }
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        final PrefixDescriptors prefdescriptor = ((PrefixCase)nlriTypeCase).getPrefixDescriptors();
        if (prefdescriptor.getIpReachabilityInformation() != null) {
            final IpPrefix prefix = prefdescriptor.getIpReachabilityInformation();
            final ByteBuf buf;
            if (prefix.getIpv4Prefix() != null) {
                buf = Unpooled.buffer(Ipv4Util.IP4_LENGTH + 1);
                ByteBufWriteUtil.writeMinimalPrefix(prefix.getIpv4Prefix(), buf);
            } else if (prefix.getIpv6Prefix() != null) {
                buf = Unpooled.buffer(Ipv6Util.IPV6_LENGTH + 1);
                ByteBufWriteUtil.writeMinimalPrefix(prefix.getIpv6Prefix(), buf);
            } else {
                buf = null;
            }
            TlvUtil.writeTLV(IP_REACHABILITY, buf, buffer);
        }
    }
}
