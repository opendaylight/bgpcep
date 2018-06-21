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
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrPrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.PrefixSidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.PrefixSidCaseBuilder;

public final class Ipv4PrefixSidParser implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    public static final int PREFIX_SID = 1158;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        final SrPrefix prefix = SrPrefixAttributesParser.parseSrPrefix(slice, protocolId);
        return new PrefixSidCaseBuilder().setAlgorithm(prefix.getAlgorithm()).setFlags(prefix.getFlags())
            .setSidLabelIndex(prefix.getSidLabelIndex()).build();
    }

    @Override
    public int getType() {
        return PREFIX_SID;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        Preconditions.checkArgument(bindingSubTlv instanceof PrefixSidCase, "Wrong BindingSubTlv instance expected", bindingSubTlv);
        final PrefixSidCase prefix = (PrefixSidCase) bindingSubTlv;
        final ByteBuf buffer = Unpooled.buffer();
        SrPrefixAttributesParser.serializePrefixAttributes(prefix.getFlags(), prefix.getAlgorithm(), prefix.getSidLabelIndex(), buffer);
        TlvUtil.writeTLV(getType(), buffer, aggregator);
    }
}
