package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.Ipv6AddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.Ipv6AddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.LocalLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.LocalLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.SidCaseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SidLabelIndexParser {

    private static final Logger LOG = LoggerFactory.getLogger(SidLabelIndexParser.class);

    static final int SID_TYPE = 1161;
    private static final int LABEL_SIZE = 3;
    private static final int SID_SIZE = 4;
    private static final int IPV6_ADD_SIZE = 16;
    private static final int OFFSET = 4;

    static ByteBuf serializeSidValue(final SidLabelIndex tlv) {
        if (tlv instanceof Ipv6AddressCase) {
            return Ipv6Util.byteBufForAddress(((Ipv6AddressCase) tlv).getIpv6Address());
        } else if (tlv instanceof LocalLabelCase) {
            return Unpooled.buffer().setMedium(0, ((LocalLabelCase) tlv).getLocalLabel().intValue());
        } else if (tlv instanceof SidCase) {
            return Unpooled.buffer().setInt(0, ((SidCase) tlv).getSid().intValue());
        }
        return null;
    }

    static SidLabelIndex parseSidSubTlv(final ByteBuf buffer) {
        final int type = buffer.readUnsignedShort();
        if (type != SID_TYPE) {
            LOG.warn("Unexpected type in SID/index/label field, expected {}, actual {}, ignoring it", SID_TYPE, type);
            return null;
        }
        final int length = buffer.readUnsignedShort();
        return parseSidLabelIndex(length, buffer);
    }

    static SidLabelIndex parseSidLabelIndex(final int length, final ByteBuf buffer) {
        if (length == LABEL_SIZE) {
            final int label = ((buffer.readUnsignedMedium() << OFFSET) >> OFFSET);
            return new LocalLabelCaseBuilder().setLocalLabel((long) label).build();
        } else if (length == SID_SIZE) {
            return new SidCaseBuilder().setSid(buffer.readUnsignedInt()).build();
        } else if (length == IPV6_ADD_SIZE) {
            return new Ipv6AddressCaseBuilder().setIpv6Address(Ipv6Util.addressForByteBuf(buffer)).build();
        }
        return null;
    }

    static int getLength(final BitArray flags, final int value, final int local) {
        if (flags.get(value) && flags.get(local)) {
            return LABEL_SIZE;
        }
        if (!flags.get(value) && !flags.get(local)) {
            return SID_SIZE;
        }
        return 0;
    }

    static void setFlags(final SidLabelIndex tlv, final BitArray flags, final int value, final int local) {
        if (tlv instanceof LocalLabelCase) {
            flags.set(value, Boolean.TRUE);
            flags.set(local, Boolean.TRUE);
        }
    }

}
