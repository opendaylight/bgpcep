package org.opendaylight.protocol.pcep.segment.routing.parsers;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcreq.pcreq.message.requests.segment.computation.p2p.rro.subobject.subobject.type.SrRroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject;

public class RequestMessageRROSrRroSubobjectParser extends
        AbstractSrSubobjectParser implements RROSubobjectSerializer {

    @Override
    public void serializeSubobject(Subobject subobject, ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof SrRroType, "Unknown subobject instance. Passed %s. Needed "
                + "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcinitiate.pcinitiate.message.requests.rro.subobject.subobject.type.SrRroType", subobject.getSubobjectType().getClass());
        final SrRroType srRroType = (SrRroType)subobject.getSubobjectType();
        final ByteBuf body = serializeSubobject(srRroType);
        RROSubobjectUtil.formatSubobject(RROSubobjectUtil.RRO_TYPE, body, buffer);
    }
}
