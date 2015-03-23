package org.opendaylight.protocol.pcep.segment.routing.parsers;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcinitiate.pcinitiate.message.requests.rro.subobject.subobject.type.SrRroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder;

public class PcinitiateMessageSrRroSubobjectParser extends
        AbstractSrSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 16;

    @Override
    public void serializeSubobject(Subobject subobject, ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof SrRroType, "Unknown subobject instance. Passed %s. Needed "
                + "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcinitiate.pcinitiate.message.requests.rro.subobject.subobject.type.SrRroType", subobject.getSubobjectType().getClass());
        final SrSubobject srSubobject = (SrSubobject) subobject.getSubobjectType();
        final ByteBuf body = serializeSubobject(srSubobject);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }

    @Override
    public Subobject parseSubobject(ByteBuf buffer)
            throws PCEPDeserializerException {
        final SubobjectBuilder subobjectBuilder = new SubobjectBuilder();
        subobjectBuilder.setSubobjectType((SrRroType)parseSrSubobject(buffer));
        return subobjectBuilder.build();
    }
}
