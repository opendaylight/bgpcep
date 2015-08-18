package parser.spi.subobjects;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKeyBuilder;

public abstract class AbstractPathKeyParser {
    public org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKey parsePathKey(final int pceIdFLength, final ByteBuf buffer) {
        final int pathKey = buffer.readUnsignedShort();
        final byte[] pceId = ByteArray.readBytes(buffer, pceIdFLength);
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(pceId));
        pBuilder.setPathKey(new PathKey(pathKey));
        return pBuilder.build();
    }

    public ByteBuf serializePathKey(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKey pk) {
        final ByteBuf body = Unpooled.buffer();
        Preconditions.checkArgument(pk.getPathKey() != null, "PathKey is mandatory.");
        writeUnsignedShort(pk.getPathKey().getValue(), body);
        Preconditions.checkArgument(pk.getPceId() != null, "PceId is mandatory.");
        body.writeBytes(pk.getPceId().getBinary());
        return body;
    }
}
