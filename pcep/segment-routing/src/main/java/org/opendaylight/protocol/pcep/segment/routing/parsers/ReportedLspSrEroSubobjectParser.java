package org.opendaylight.protocol.pcep.segment.routing.parsers;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.network.topology.topology.node.path.computation.client.reported.lsp.path.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.network.topology.topology.node.path.computation.client.reported.lsp.path.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;

public class ReportedLspSrEroSubobjectParser extends AbstractSrSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 11;

    @Override
    public void serializeSubobject(Subobject subobject, ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof SrEroType, "Unknown subobject instance. Passed %s. Needed "
                + "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.network.topology.topology.node.path.computation.client.reported.lsp.path.ero.subobject.subobject.type.SrEroType", subobject.getSubobjectType().getClass());
        final SrEroType srEroType = (SrEroType)subobject.getSubobjectType();
        final ByteBuf body = serializeSubobject(srEroType);
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }

    @Override
    public Subobject parseSubobject(ByteBuf buffer, boolean loose)
            throws PCEPDeserializerException {
        final SubobjectBuilder subobjectBuilder = new SubobjectBuilder();
        final SrSubobject srSubobject = parseSrSubobject(buffer);
        final SrEroTypeBuilder eroTypeBuilder = new SrEroTypeBuilder(srSubobject);
        subobjectBuilder.setSubobjectType(eroTypeBuilder.build());
        subobjectBuilder.setLoose(loose);
        return subobjectBuilder.build();
    }
}
