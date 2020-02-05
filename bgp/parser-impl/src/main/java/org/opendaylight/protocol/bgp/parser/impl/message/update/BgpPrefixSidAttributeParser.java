/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvRegistry;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.BgpPrefixSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.BgpPrefixSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvsBuilder;

public final class BgpPrefixSidAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 40;

    private final BgpPrefixSidTlvRegistry reg;

    public BgpPrefixSidAttributeParser(final BgpPrefixSidTlvRegistry registry) {
        this.reg = requireNonNull(registry);
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final BgpPrefixSid prefixSid = pathAttributes.getBgpPrefixSid();
        if (prefixSid == null) {
            return;
        }
        for (final BgpPrefixSidTlvs tlv : prefixSid.getBgpPrefixSidTlvs()) {
            this.reg.serializeBgpPrefixSidTlv(tlv.getBgpPrefixSidTlv(), byteAggregator);
        }
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException, BGPParsingException {
        final BgpPrefixSidBuilder sid = new BgpPrefixSidBuilder();
        final List<BgpPrefixSidTlvs> tlvList = new ArrayList<>();
        while (buffer.isReadable()) {
            tlvList.add(new BgpPrefixSidTlvsBuilder()
                .setBgpPrefixSidTlv(this.reg.parseBgpPrefixSidTlv(buffer.readUnsignedByte(), buffer))
                .build());
        }
        builder.setBgpPrefixSid(sid.setBgpPrefixSidTlvs(tlvList).build());
    }

}
