/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleBindingSubTlvsRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrBindingSidLabels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrBindingSidLabelsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.BindingSubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.flags.IsisBindingFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.flags.IsisBindingFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.flags.OspfBindingFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.flags.OspfBindingFlagsCaseBuilder;

public final class BindingSidLabelParser {
    /* Flags */
    private static final int FLAGS_SIZE = 8;
    private static final int AFI = 0;
    private static final int MIRROR_CONTEXT = 1;
    private static final int MIRROR_CONTEXT_OSPF = 0;
    private static final int SPREAD_TLV = 2;
    private static final int LEAKED = 3;
    private static final int ATTACHED = 4;

    /* SID Label Tlv types */
    private static final int RESERVED_BINDING_SID = 2;

    private BindingSidLabelParser() {
        throw new UnsupportedOperationException();
    }

    public static SrBindingSidLabels parseBindingSidLabel(final ByteBuf buffer, final ProtocolId protocolId) {
        final SrBindingSidLabelsBuilder bindingSid = new SrBindingSidLabelsBuilder();
        bindingSid.setWeight(new Weight(buffer.readUnsignedByte()));
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        bindingSid.setFlags(parseBindingSidFlags(flags, protocolId));
        buffer.skipBytes(RESERVED_BINDING_SID);
        bindingSid.setBindingSubTlvs(SimpleBindingSubTlvsRegistry.getInstance().parseBindingSubTlvs(buffer, protocolId));
        return bindingSid.build();
    }

    private static Flags parseBindingSidFlags(final BitArray flags, final ProtocolId protocol) {
        switch (protocol) {
        case IsisLevel1:
        case IsisLevel2:
            return new IsisBindingFlagsCaseBuilder().setAddressFamily(flags.get(AFI)).setMirrorContext(flags.get(MIRROR_CONTEXT))
                .setSpreadTlv(flags.get(SPREAD_TLV)).setLeakedFromLevel2(flags.get(LEAKED)).setAttachedFlag(flags.get(ATTACHED)).build();
        case Ospf:
        case OspfV3:
            return new OspfBindingFlagsCaseBuilder().setMirroring(flags.get(MIRROR_CONTEXT_OSPF)).build();
        default:
            return null;
        }
    }

    public static void serializeBindingSidAttributes(final Weight weight, final Flags flags, final List<BindingSubTlvs> bindingSubTlvs, final ByteBuf aggregator) {
        aggregator.writeByte(weight.getValue());
        final BitArray bitFlags = serializeBindingSidFlags(flags);
        bitFlags.toByteBuf(aggregator);
        aggregator.writeZero(RESERVED_BINDING_SID);
        SimpleBindingSubTlvsRegistry.getInstance().serializeBindingSubTlvs(bindingSubTlvs, aggregator);
    }

    private static BitArray serializeBindingSidFlags(final Flags flags) {
        final BitArray bitFlags = new BitArray(FLAGS_SIZE);
        if (flags instanceof IsisBindingFlagsCase) {
            final IsisBindingFlagsCase isisFlags = (IsisBindingFlagsCase) flags;
            bitFlags.set(AFI, isisFlags.isAddressFamily());
            bitFlags.set(MIRROR_CONTEXT, isisFlags.isMirrorContext());
            bitFlags.set(SPREAD_TLV, isisFlags.isSpreadTlv());
            bitFlags.set(LEAKED, isisFlags.isLeakedFromLevel2());
            bitFlags.set(ATTACHED, isisFlags.isAttachedFlag());
        } else if (flags instanceof OspfBindingFlagsCase) {
            final OspfBindingFlagsCase ospfFlags = (OspfBindingFlagsCase) flags;
            bitFlags.set(MIRROR_CONTEXT_OSPF, ospfFlags.isMirroring());
        }
        return bitFlags;
    }
}
