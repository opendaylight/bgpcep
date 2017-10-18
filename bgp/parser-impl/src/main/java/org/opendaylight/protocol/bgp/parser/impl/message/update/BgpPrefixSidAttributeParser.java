/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.BgpPrefixSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.BgpPrefixSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class BgpPrefixSidAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 40;

    private final BgpPrefixSidTlvRegistry reg;

    public BgpPrefixSidAttributeParser(final BgpPrefixSidTlvRegistry registry) {
        this.reg = requireNonNull(registry);
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final BgpPrefixSid prefixSid = pathAttributes.getBgpPrefixSid();
        if (prefixSid == null) {
            return;
        }
        for (final BgpPrefixSidTlvs tlv : prefixSid.getBgpPrefixSidTlvs()) {
            this.reg.serializeBgpPrefixSidTlv(tlv.getBgpPrefixSidTlv(), byteAggregator);
        }
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) throws BGPDocumentedException, BGPParsingException {
        final BgpPrefixSidBuilder sid = new BgpPrefixSidBuilder();
        final List<BgpPrefixSidTlvs> tlvList = new ArrayList<>();
        while (buffer.isReadable()) {
            final BgpPrefixSidTlv tlv = this.reg.parseBgpPrefixSidTlv(buffer.readUnsignedByte(), buffer);
            tlvList.add(new BgpPrefixSidTlvsBuilder().setBgpPrefixSidTlv(tlv).build());
        }
        builder.setBgpPrefixSid(sid.setBgpPrefixSidTlvs(tlvList).build());
    }

}
