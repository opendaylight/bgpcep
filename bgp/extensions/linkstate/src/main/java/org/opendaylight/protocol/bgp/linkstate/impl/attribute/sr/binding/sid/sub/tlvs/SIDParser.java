/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SidLabelIndexParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.SidLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.SidLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.SidLabelIndex;

public final class SIDParser implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    public static final int SID_TYPE = 1161;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        final SidLabelIndex sid = SidLabelIndexParser.parseSidLabelIndex(SidLabelIndexParser.Size.forValue(
            slice.readableBytes()), slice);
        return new SidLabelCaseBuilder().setSidLabelIndex(sid).build();
    }

    @Override
    public int getType() {
        return SID_TYPE;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        checkArgument(bindingSubTlv instanceof SidLabelCase, "Wrong BindingSubTlv instance expected",
            bindingSubTlv);
        TlvUtil.writeTLV(getType(), SidLabelIndexParser.serializeSidValue(
            ((SidLabelCase) bindingSubTlv).getSidLabelIndex()), aggregator);
    }
}
