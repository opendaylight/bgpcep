/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.parser.object;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.overload.object.Overload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.overload.object.OverloadBuilder;

public class PCEPOverloadObjectParser extends CommonObjectParser implements ObjectSerializer {

    private static final int CLASS = 27;
    private static final int TYPE = 1;
    private static final int RESERVED = 1;
    private static final int FLAGS = RESERVED;
    private static final int BODY_SIZE = RESERVED + FLAGS + ByteBufWriteUtil.SHORT_BYTES_LENGTH;

    public PCEPOverloadObjectParser() {
        super(CLASS, TYPE);
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Overload,
            "Wrong instance of PCEPObject. Passed %s. Needed OverloadObject.", object.getClass());
        final Overload overload = (Overload) object;
        final ByteBuf body = Unpooled.buffer(BODY_SIZE);
        body.writeZero(RESERVED + FLAGS);
        ByteBufWriteUtil.writeUnsignedShort(overload.getDuration(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        final OverloadBuilder builder = new OverloadBuilder();
        buffer.readBytes(RESERVED + FLAGS);
        builder.setDuration(buffer.readUnsignedShort());
        return builder.build();
    }
}
